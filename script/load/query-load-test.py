# -*- coding: utf-8 -*-
"""
TrainTicketing 查询路径压测：验证「余票查询直查 MySQL」在真实流量形态下的容量。

背景：真实购票流量里 查询:下单 ≈ 100:1（用户疯狂刷新余票页，只有少数人下单）。
当前实现 query-remaining 直查 MySQL（每座位一次 NOT EXISTS 占用判断），
Redis 缓存只服务下单预扣。本脚本回答：查询路径能扛多少 QPS？

数据策略（形态对 > 绝对量大）：
  - 1 辆大库存车次（--seats 座位）并已售 --sold 张（NOT EXISTS 有真实工作量）
  - --trains 辆小车型复用同一对车站，供车次列表查询（ticket/query）扫描
  - 已售占用直接 SQL 批量插入（下单 API 受锁串行限制太慢）

用法（需全套容器在跑）：
  python script/load/query-load-test.py --duration 30 --rate 100
  python script/load/query-load-test.py --duration 30 --rate 300 --seats 2000 --sold 1500

工作负载：按 --remaining-ratio（默认 70%）混发 两个只读接口：
  GET /business/ticket/query-remaining   （单排班区间余票，扫主车次全部座位）
  GET /business/ticket/query             （按车站对+日期列车次列表）
"""
import argparse
import json
import statistics
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from threading import Event

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

API = None
TOKEN = [None]
ARGS = None
RUN_DATE = None


def call(method, path, form=None, timeout=30):
    data, headers = None, {}
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    if TOKEN[0]:
        headers["Authorization"] = "Bearer " + TOKEN[0]
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            body = r.read().decode()
    except urllib.error.HTTPError as e:
        return False, e.read().decode()[:120]
    except Exception as e:
        return False, repr(e)[:120]
    try:
        d = json.loads(body)
        return bool(d.get("success")), body
    except Exception:
        return False, body[:120]


def must(method, path, form=None):
    ok, raw = call(method, path, form)
    if not ok:
        raise SystemExit("前置步骤失败 %s %s => %s" % (method, path, raw[:160]))
    return raw


def jcontent(raw):
    return json.loads(raw)["content"]


def bulk_sql(sql):
    r = subprocess.run(["docker", "exec", "tt-mysql", "mysql", "-uroot", "-p123456",
                        "train_ticketing", "-e", sql],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit("批量 SQL 失败: %s" % (r.stderr or r.stdout)[:300])


def setup():
    """建 1 辆大库存车次（卖出 --sold 张）+ --trains 辆小车型，返回查询目标集合"""
    code = jcontent(must("POST", "/member/member/send-code", form={"mobile": ARGS.mobile}))
    login = jcontent(must("POST", "/member/member/login", form={"mobile": ARGS.mobile, "code": code}))
    TOKEN[0] = login["token"]

    sfx = str(int(time.time() * 10) % 10_000_000)
    sa = jcontent(must("POST", "/business/admin/station/save",
                       form={"name": "查东" + sfx, "namePinyin": "cd", "namePy": "CD", "city": "查市"}))
    sb = jcontent(must("POST", "/business/admin/station/save",
                       form={"name": "查中" + sfx, "namePinyin": "cz", "namePy": "CZ", "city": "查市"}))
    sc = jcontent(must("POST", "/business/admin/station/save",
                       form={"name": "查西" + sfx, "namePinyin": "cx", "namePy": "CX", "city": "查市"}))

    def build_train(code_, stock):
        tid = jcontent(must("POST", "/business/admin/train/save",
                            form={"code": code_, "type": "1", "startStationId": sa,
                                  "endStationId": sc, "startTime": "08:00", "endTime": "09:30"}))
        for idx, (sid, arr, lv) in enumerate([(sa, None, "08:00"), (sb, "08:40", "08:42"), (sc, "09:30", None)], 1):
            ts = {"trainId": tid, "stationId": sid, "stationIndex": idx}
            if arr:
                ts["arriveTime"] = arr
            if lv:
                ts["leaveTime"] = lv
            must("POST", "/business/admin/train-station/save", form=ts)
        car = jcontent(must("POST", "/business/admin/train-carriage/save",
                            form={"trainId": tid, "carriageIndex": 1, "seatType": "3", "seatCount": stock}))
        must("POST", "/business/admin/train-seat/generate?carriageId=%s" % car, form={})
        must("POST", "/business/admin/train-price/save",
             form={"trainId": tid, "seatType": "3", "price": "100.00"})
        daily = jcontent(must("POST", "/business/admin/daily-train/save",
                              form={"trainId": tid, "runDate": RUN_DATE}))
        must("POST", "/business/admin/daily-train-seat/generate?dailyTrainId=%s" % daily, form={})
        return daily

    print("[前置] 主车次：%d 座 ..." % ARGS.seats)
    main_daily = build_train("T%04d" % (int(time.time()) % 10000), ARGS.seats)

    if ARGS.sold > 0:
        # 批量造已售占用：1 个大订单 + sold 条明细（覆盖 A-C 全区间 1~3），
        # 明细 id 用 8.9e18 - seat.id 保证唯一且不与雪花冲突；status 对 NOT EXISTS 判定无影响
        oid = 8800000000000000000 + int(time.time()) % 100000000
        bulk_sql(
            "INSERT INTO train_order (id, order_no, member_id, daily_train_id, train_id, "
            "depart_station_id, arrive_station_id, run_date, status, total_amount) VALUES "
            "(%d, 'BULK%s', 999, %s, %s, %s, %s, '%s', '1', 100.00);"
            % (oid, sfx, main_daily, 0, sa, sc, RUN_DATE))
        bulk_sql(
            "INSERT INTO train_order_item (id, order_id, passenger_id, passenger_name, id_card, "
            "daily_train_seat_id, seat_type, price, depart_index, arrive_index) "
            "SELECT 8900000000000000000 - s.id, %d, 999, '批量乘客', '110101199001019999', s.id, '3', "
            "100.00, 1, 3 FROM daily_train_seat s WHERE s.daily_train_id = %s AND s.seat_type = '3' "
            "LIMIT %d;" % (oid, main_daily, ARGS.sold))
        print("[前置] 已批量售出 %d 张（train_order_item）" % ARGS.sold)

    dailies = [main_daily]
    if ARGS.trains > 0:
        print("[前置] 建列表查询用车次 × %d（各 %d 座）..." % (ARGS.trains, ARGS.train_seats))
        for i in range(ARGS.trains):
            dailies.append(build_train("T%04d" % ((int(time.time()) + i + 1) % 10000), ARGS.train_seats))

    print("[前置] 数据链就绪：主排班=%s + 列表车次 %d 辆" % (main_daily, len(dailies) - 1))
    return {"sa": sa, "sc": sc, "dailies": dailies}


def fire(ctx, duration, rate):
    """按 rate/s 持续混发两个查询接口（绝对节拍），返回 [(endpoint, ok, ms)]"""
    remaining_daily = ctx["dailies"]
    list_path = "/business/ticket/query?fromStationId=%s&toStationId=%s&runDate=%s" % (
        ctx["sa"], ctx["sc"], RUN_DATE)
    counter = [0]
    gate = Event()
    gate.set()
    results = []

    def hit(kind, path):
        t0 = time.perf_counter()
        ok, raw = call("GET", path)
        ms = (time.perf_counter() - t0) * 1000
        results.append((kind, ok, ms, raw if not ok else None))

    def pick():
        i = counter[0]
        counter[0] += 1
        if i % 100 < ARGS.remaining_ratio:
            d = remaining_daily[i % len(remaining_daily)]
            return "remaining", ("/business/ticket/query-remaining?dailyTrainId=%s"
                                 "&departStationId=%s&arriveStationId=%s" % (d, ctx["sa"], ctx["sc"]))
        return "list", list_path

    with ThreadPoolExecutor(max_workers=rate * 3) as pool:
        t0 = time.perf_counter()
        sent = 0
        next_tick = t0
        while next_tick - t0 < duration:
            if next_tick > time.perf_counter():
                time.sleep(next_tick - time.perf_counter())
            kind, path = pick()
            pool.submit(hit, kind, path)
            sent += 1
            next_tick += 1.0 / rate
        print("[压测] 已按 %d req/s 施压 %ds（发出 %d 个查询），等待收尾 ..." % (rate, duration, sent))
        pool.shutdown(wait=True)
    return results, time.perf_counter() - t0


def pct(sorted_vals, p):
    if not sorted_vals:
        return 0.0
    k = min(len(sorted_vals) - 1, int(round((p / 100.0) * (len(sorted_vals) - 1))))
    return sorted_vals[k]


def main():
    global API, ARGS, RUN_DATE
    ap = argparse.ArgumentParser(description="TrainTicketing 查询路径压测（读多写少真实形态）")
    ap.add_argument("--url", default="http://127.0.0.1:8000", help="网关地址")
    ap.add_argument("--mobile", default="13900000002", help="压测账号手机号")
    ap.add_argument("--duration", type=int, default=30, help="施压时长（秒）")
    ap.add_argument("--rate", type=int, default=100, help="查询 QPS")
    ap.add_argument("--seats", type=int, default=2000, help="主车次座位数（NOT EXISTS 扫描规模）")
    ap.add_argument("--sold", type=int, default=1000, help="主车次预置已售张数（批量 SQL 直插）")
    ap.add_argument("--trains", type=int, default=10, help="车次列表查询用的小车型数量")
    ap.add_argument("--train-seats", type=int, default=100, help="小车型座位数")
    ap.add_argument("--remaining-ratio", type=int, default=70, help="query-remaining 占比 %%")
    ARGS = ap.parse_args()
    API = ARGS.url
    import datetime
    RUN_DATE = (datetime.date.today() + datetime.timedelta(days=7)).isoformat()

    print("=== TrainTicketing 查询路径压测 ===")
    print("目标: %s  QPS: %d  时长: %ds  主车次: %d 座/已售 %d  列表车次: %d" % (
        API, ARGS.rate, ARGS.duration, ARGS.seats, ARGS.sold, ARGS.trains))
    ctx = setup()
    results, wall = fire(ctx, ARGS.duration, ARGS.rate)

    print("\n----- 结果（按接口分列，单位 ms）-----")
    fail = 0
    for kind in ("remaining", "list"):
        rows = [r for r in results if r[0] == kind]
        if not rows:
            continue
        lats = sorted(r[2] for r in rows)
        errs = [r for r in rows if not r[1]]
        fail += len(errs)
        print("%-10s %6d 次  avg=%6.0f p50=%6.0f p90=%6.0f p99=%6.0f max=%6.0f  失败=%d" % (
            kind, len(rows), statistics.mean(lats), pct(lats, 50), pct(lats, 90),
            pct(lats, 99), lats[-1], len(errs)))
        for sample in {r[3] for r in errs}:
            print("    失败样例: %s" % (sample or "")[:110])
    print("合计      : %d 次  实际 %.1f req/s  失败率 %.2f%%" % (
        len(results), len(results) / wall if wall else 0, 100.0 * fail / max(1, len(results))))
    print("\n提示: 打开 Grafana 看 MySQL 侧压力（连接池 active/pending、business CPU）")
    sys.exit(0 if fail == 0 else 1)


if __name__ == "__main__":
    main()
