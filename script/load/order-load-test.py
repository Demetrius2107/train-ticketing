# -*- coding: utf-8 -*-
"""
TrainTicketing HTTP 层并发压测：经网关(8000) -> JWT -> business 容器 -> MySQL/Redis 全链路。

用法（需 docker compose 全套在跑；压的是容器里的真实服务，不是本地 JVM）：
  python script/load/order-load-test.py                          # 默认：50 库存 / 100 并发 / 200 请求
  python script/load/order-load-test.py --stock 20 --concurrency 200 --total 500

脚本流程：
  1. 发码登录（容器 mock 模式验证码直返）+ 建乘车人
  2. 造一条完整车次数据链（3 站 / 1 车厢 / stock 个二等座 / 排班 / 当日座位+余票缓存）
  3. total 个下单请求以 concurrency 并发同起点放行（每个请求独立幂等键）
  4. 输出：成功/余票不足/锁忙分布、延迟分位（p50/p90/p99）、吞吐
  5. 防超卖断言：成功数 <= 库存 且 终态余票 == 库存 - 成功数

压测期间建议开着 Grafana（http://localhost:3000，admin/admin）看容器 CPU/JVM/连接池曲线。
"""
import argparse
import json
import re
import statistics
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from threading import Event

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

API = None
TOKEN = None
PASSENGER = None
RUN_DATE = None


def call(method, path, form=None, jbody=None, timeout=30):
    """同步 HTTP；返回 (http_ok, parsed_json_or_None, raw_text)"""
    data, headers = None, {}
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    if jbody is not None:
        data = json.dumps(jbody, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json;charset=UTF-8"
    if TOKEN[0]:
        headers["Authorization"] = "Bearer " + TOKEN[0]
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            body = r.read().decode()
    except urllib.error.HTTPError as e:
        return False, None, e.read().decode()[:200]
    except Exception as e:
        return False, None, repr(e)[:200]
    try:
        return True, json.loads(body), body
    except Exception:
        return False, None, body[:200]


def must(method, path, form=None, jbody=None):
    ok, d, raw = call(method, path, form, jbody)
    if not ok or not d or not d.get("success"):
        raise SystemExit("前置步骤失败 %s %s => %s" % (method, path, raw[:200]))
    return d["content"]


def setup(stock):
    """登录 + 乘车人 + 完整车次数据链，返回下单所需的上下文"""
    code = must("POST", "/member/member/send-code", form={"mobile": ARGS.mobile})
    if not re.fullmatch(r"\d{6}", code or ""):
        raise SystemExit("send-code 未直返验证码（容器需开启 member.sms.mock-return-code，"
                         "即用 docker profile 跑）：%r" % code)
    login = must("POST", "/member/member/login", form={"mobile": ARGS.mobile, "code": code})
    TOKEN[0] = login["token"]
    pid = must("POST", "/member/passenger/save",
               jbody={"memberId": login["id"], "name": "压测乘客", "idCard": "110101199001019999", "type": "1"})
    PASSENGER.append(pid)

    sfx = str(int(time.time() * 10) % 10_000_000)
    sa = must("POST", "/business/admin/station/save",
              form={"name": "压测东" + sfx, "namePinyin": "cyd", "namePy": "CYD", "city": "压测市"})
    sb = must("POST", "/business/admin/station/save",
              form={"name": "压测中" + sfx, "namePinyin": "cyz", "namePy": "CYZ", "city": "压测市"})
    sc = must("POST", "/business/admin/station/save",
              form={"name": "压测西" + sfx, "namePinyin": "cyx", "namePy": "CYX", "city": "压测市"})
    tid = must("POST", "/business/admin/train/save",
               form={"code": "T%04d" % (int(time.time()) % 10000), "type": "1",
                     "startStationId": sa, "endStationId": sc, "startTime": "08:00", "endTime": "09:30"})
    for idx, (sid, arr, lv) in enumerate([(sa, None, "08:00"), (sb, "08:40", "08:42"), (sc, "09:30", None)], 1):
        ts = {"trainId": tid, "stationId": sid, "stationIndex": idx}
        if arr:
            ts["arriveTime"] = arr
        if lv:
            ts["leaveTime"] = lv
        must("POST", "/business/admin/train-station/save", form=ts)
    car = must("POST", "/business/admin/train-carriage/save",
               form={"trainId": tid, "carriageIndex": 1, "seatType": "3", "seatCount": stock})
    got = must("POST", "/business/admin/train-seat/generate?carriageId=%s" % car, form={})
    assert int(got) == stock, "座位档案生成数 %s != 库存 %s" % (got, stock)
    must("POST", "/business/admin/train-price/save",
         form={"trainId": tid, "seatType": "3", "price": "100.00"})
    daily = must("POST", "/business/admin/daily-train/save", form={"trainId": tid, "runDate": RUN_DATE})
    got = must("POST", "/business/admin/daily-train-seat/generate?dailyTrainId=%s" % daily, form={})
    assert int(got) == stock, "当日座位生成数 %s != 库存 %s" % (got, stock)
    print("[前置] 数据链就绪：dailyTrainId=%s 库存=%d 乘车人=%s" % (daily, stock, pid))
    return {"daily": daily, "sa": sa, "sc": sc}


def fire(ctx, total, concurrency):
    """同起点放行 total 个下单请求，返回逐请求结果 (ok, latency_ms, message)"""
    start_gate = Event()
    results = []

    def one():
        body = {"idempotentKey": str(uuid.uuid4()), "dailyTrainId": ctx["daily"],
                "departStationId": ctx["sa"], "arriveStationId": ctx["sc"],
                "runDate": RUN_DATE, "seatType": "3",
                "passengers": [{"passengerId": PASSENGER[0], "name": "压测乘客", "idCard": "110101199001019999"}]}
        start_gate.wait()
        t0 = time.perf_counter()
        ok, d, raw = call("POST", "/business/order/save", jbody=body)
        ms = (time.perf_counter() - t0) * 1000
        if ok and d and d.get("success"):
            results.append((True, ms, None))
        else:
            msg = (d.get("message") if d else None) or raw[:80]
            results.append((False, ms, msg))

    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures = [pool.submit(one) for _ in range(total)]
        time.sleep(0.8)  # 等全部 worker 就位再统一放行，制造同一瞬间的高压
        t0 = time.perf_counter()
        start_gate.set()
        for f in futures:
            f.result()
    return results, time.perf_counter() - t0


def fire_sustain(ctx, duration, rate):
    """持续模式：按固定速率（请求/秒）打 duration 秒，观察 Grafana 资源曲线用。
    库存需配大（如 --stock 5000），否则很快全被打成余票不足。"""
    start_gate = Event()
    start_gate.set()
    results = []
    interval = 1.0 / rate

    def one():
        body = {"idempotentKey": str(uuid.uuid4()), "dailyTrainId": ctx["daily"],
                "departStationId": ctx["sa"], "arriveStationId": ctx["sc"],
                "runDate": RUN_DATE, "seatType": "3",
                "passengers": [{"passengerId": PASSENGER[0], "name": "压测乘客", "idCard": "110101199001019999"}]}
        t0 = time.perf_counter()
        ok, d, raw = call("POST", "/business/order/save", jbody=body)
        ms = (time.perf_counter() - t0) * 1000
        if ok and d and d.get("success"):
            results.append((True, ms, None))
        else:
            msg = (d.get("message") if d else None) or raw[:80]
            results.append((False, ms, msg))

    with ThreadPoolExecutor(max_workers=rate * 5) as pool:
        t0 = time.perf_counter()
        sent = 0
        next_tick = t0
        while (tick_at := next_tick) - t0 < duration:
            # 每个 interval 提交 1 个请求 → 精确 rate/s；用绝对节拍避免计时漂移
            if tick_at > time.perf_counter():
                time.sleep(tick_at - time.perf_counter())
            pool.submit(one)
            sent += 1
            next_tick = tick_at + interval
        print("[压测] 已按 %d req/s 持续施压 %ds（发出 %d 个请求），等待在途收尾 ..." % (rate, duration, sent))
        pool.shutdown(wait=True)
    return results, time.perf_counter() - t0


def pct(sorted_vals, p):
    if not sorted_vals:
        return 0.0
    k = min(len(sorted_vals) - 1, int(round((p / 100.0) * (len(sorted_vals) - 1))))
    return sorted_vals[k]


def main():
    global API, RUN_DATE, ARGS
    ap = argparse.ArgumentParser(description="TrainTicketing HTTP 并发压测（经网关打容器服务）")
    ap.add_argument("--url", default="http://127.0.0.1:8000", help="网关地址")
    ap.add_argument("--mobile", default="13900000001", help="压测账号手机号")
    ap.add_argument("--stock", type=int, default=50, help="本次车次二等座库存（burst 模式=库存；sustain 模式请配大，如 5000）")
    ap.add_argument("--concurrency", type=int, default=100, help="burst 模式并发线程数")
    ap.add_argument("--total", type=int, default=200, help="burst 模式总请求数")
    ap.add_argument("--sustain", type=int, default=0, metavar="秒数",
                    help="持续模式：按 --rate 打 N 秒（观察 Grafana 曲线用），与 --total/--concurrency 互斥")
    ap.add_argument("--rate", type=int, default=50, metavar="每秒请求数", help="持续模式的施压速率")
    ARGS = ap.parse_args()
    API = ARGS.url
    TOKEN = [None]
    PASSENGER = []
    import datetime
    RUN_DATE = (datetime.date.today() + datetime.timedelta(days=7)).isoformat()
    globals()["TOKEN"] = TOKEN
    globals()["PASSENGER"] = PASSENGER

    print("=== TrainTicketing HTTP 并发压测 ===")
    if ARGS.sustain:
        print("模式: 持续  目标: %s  库存: %d  速率: %d req/s  时长: %ds" % (
            API, ARGS.stock, ARGS.rate, ARGS.sustain))
    else:
        print("模式: 冲击  目标: %s  库存: %d  并发: %d  总请求: %d" % (
            API, ARGS.stock, ARGS.concurrency, ARGS.total))
    ctx = setup(ARGS.stock)
    if ARGS.sustain:
        results, wall = fire_sustain(ctx, ARGS.sustain, ARGS.rate)
    else:
        print("\n[压测] 放行 %d 个并发下单请求 ..." % ARGS.total)
        results, wall = fire(ctx, ARGS.total, ARGS.total and ARGS.concurrency)

    succ = [r for r in results if r[0]]
    fail = [r for r in results if not r[0]]
    buckets = {}
    for _, _, msg in fail:
        if msg and "余票" in msg:
            key = "余票不足(预期)"
        elif msg and ("忙" in msg or "锁" in msg or "繁忙" in msg or "人数过多" in msg):
            key = "获取锁超时(预期)"
        else:
            key = "其他失败(需关注)"
        buckets[key] = buckets.get(key, 0) + 1

    all_lat = sorted(r[1] for r in results)
    succ_lat = sorted(r[1] for r in succ)
    print("\n----- 结果 -----")
    print("成功下单  : %d / %d" % (len(succ), len(results)))
    for k, v in sorted(buckets.items()):
        print("%s: %d" % (k, v))
        if k.startswith("其他"):
            for m in {r[2] for r in fail if r[2] and ("余票" not in r[2] and "忙" not in r[2] and "锁" not in r[2])}:
                print("    样例: %s" % m[:100])
    print("耗时      : %.2fs   吞吐: %.1f req/s" % (wall, len(results) / wall if wall else 0))
    print("延迟(全部): avg=%.0fms p50=%.0f p90=%.0f p99=%.0f max=%.0f" % (
        statistics.mean(all_lat) if all_lat else 0,
        pct(all_lat, 50), pct(all_lat, 90), pct(all_lat, 99), all_lat[-1] if all_lat else 0))
    if succ_lat:
        print("延迟(成功): avg=%.0fms p50=%.0f p90=%.0f p99=%.0f" % (
            statistics.mean(succ_lat), pct(succ_lat, 50), pct(succ_lat, 90), pct(succ_lat, 99)))

    rem = must("GET", "/business/ticket/query-remaining?dailyTrainId=%s&departStationId=%s&arriveStationId=%s"
               % (ctx["daily"], ctx["sa"], ctx["sc"]))
    remaining = int(rem[0]["remainingCount"]) if rem else 0
    print("\n----- 防超卖断言 -----")
    print("库存=%d 成功=%d 终态余票=%d（期望 %d）" % (ARGS.stock, len(succ), remaining, ARGS.stock - len(succ)))
    ok1 = len(succ) <= ARGS.stock
    ok2 = remaining == ARGS.stock - len(succ)
    print("结论      : %s（成功数不超库存: %s；余票与成票一致: %s）" % ("PASS ✔" if ok1 and ok2 else "FAIL ✘", ok1, ok2))
    print("\n提示: 压测数据已进入 Prometheus，打开 Grafana(http://localhost:3000 admin/admin) 看曲线")
    sys.exit(0 if ok1 and ok2 else 1)


if __name__ == "__main__":
    main()
