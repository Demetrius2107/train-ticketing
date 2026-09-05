# -*- coding: utf-8 -*-
"""
Web 页面联调演示数据一键造数：造 4 个真实命名的车站 + 2 趟车次（今日+7）。
页面流程：cd web && npm run dev → 登录 → 查询"北京南→上海虹桥" → 预订 → 我的订单。
可重复执行：车站按名称复用，车次编号带日期后缀不冲突。
"""
import datetime
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request

API = "http://127.0.0.1:8000"
MOBILE = "13900000099"
RUN_DATE = (datetime.date.today() + datetime.timedelta(days=7)).isoformat()


def call(method, path, form=None, token=None):
    data, headers = None, {}
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            body = r.read().decode()
    except urllib.error.HTTPError as e:
        body = e.read().decode()
    d = json.loads(body)
    assert d.get("success"), "%s %s => %s" % (method, path, body[:150])
    return d["content"]


code = call("POST", "/member/member/send-code", form={"mobile": MOBILE})
assert code and re.fullmatch(r"\d{6}", code), "需要容器 mock 验证码模式"
token = call("POST", "/member/member/login", form={"mobile": MOBILE, "code": code})["token"]

all_stations = call("GET", "/business/admin/station/query") or []


def station(name, pinyin, py, city):
    for s in all_stations:
        if s["name"] == name:
            return s["id"]
    return call("POST", "/business/admin/station/save", token=token,
                form={"name": name, "namePinyin": pinyin, "namePy": py, "city": city})


s_bj = station("北京南", "beijingnan", "BJN", "北京")
s_jn = station("济南西", "jinanxi", "JNX", "济南")
s_nj = station("南京南", "nanjingnan", "NJN", "南京")
s_sh = station("上海虹桥", "shanghaihongqiao", "SHQ", "上海")
print("车站就绪：北京南 济南西 南京南 上海虹桥")


def build_train(code_, stations_, stock):
    tid = call("POST", "/business/admin/train/save", token=token,
               form={"code": code_, "type": "1", "startStationId": stations_[0][0],
                     "endStationId": stations_[-1][0], "startTime": "08:00",
                     "endTime": "%02d:%02d" % (8 + len(stations_) - 1, 0)})
    for idx, (sid, arr, lv) in enumerate(stations_, 1):
        form = {"trainId": tid, "stationId": sid, "stationIndex": idx}
        if arr:
            form["arriveTime"] = arr
        if lv:
            form["leaveTime"] = lv
        call("POST", "/business/admin/train-station/save", token=token, form=form)
    car = call("POST", "/business/admin/train-carriage/save", token=token,
               form={"trainId": tid, "carriageIndex": 1, "seatType": "3", "seatCount": stock})
    call("POST", "/business/admin/train-seat/generate?carriageId=%s" % car, token=token, form={})
    call("POST", "/business/admin/train-price/save", token=token,
         form={"trainId": tid, "seatType": "3", "price": "553.00"})
    daily = call("POST", "/business/admin/daily-train/save", token=token,
                 form={"trainId": tid, "runDate": RUN_DATE})
    n = call("POST", "/business/admin/daily-train-seat/generate?dailyTrainId=%s" % daily,
             token=token, form={})
    print("车次 %s（%s）：%d 个座位，排班 %s" % (code_, " → ".join(s[1] for s in stations_), n, RUN_DATE))


sfx = time.strftime("%m%d")
build_train("G1" + sfx, [(s_bj, None, "08:00"), (s_jn, "09:10", "09:12"),
                         (s_nj, "10:40", "10:42"), (s_sh, "11:58", None)], 500)
build_train("G3" + sfx, [(s_bj, None, "13:00"), (s_jn, "14:10", "14:12"),
                         (s_nj, "15:40", "15:42"), (s_sh, "16:58", None)], 300)
print("\n完成。启动前端：cd web && npm run dev → 登录任意手机号 → 查询 北京南 → 上海虹桥（%s）" % RUN_DATE)
