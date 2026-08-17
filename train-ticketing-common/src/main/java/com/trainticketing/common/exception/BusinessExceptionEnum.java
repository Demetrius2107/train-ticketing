package com.trainticketing.common.exception;

public enum BusinessExceptionEnum {
  MEMBER_MOBILE_EXIST("手机号已注册"),
  MEMBER_MOBILE_NOT_EXIST("请先获取短信验证码"),
  MEMBER_MOBILE_CODE_ERROR("短信验证码错误"),
  MEMBER_MOBILE_CODE_EXPIRED("短信验证码已过期"),
  MEMBER_PASSENGER_TYPE_INVALID("旅客类型不合法"),
  MEMBER_PASSENGER_NOT_EXIST("乘车人不存在"),

  BUSINESS_STATION_NAME_UNIQUE_ERROR("车站已存在"),
  BUSINESS_TRAIN_CODE_UNIQUE_ERROR("车次编号已存在"),
  BUSINESS_TRAIN_STATION_INDEX_UNIQUE_ERROR("同车次站序已存在"),
  BUSINESS_TRAIN_STATION_NAME_UNIQUE_ERROR("同车次站名已存在"),
  BUSINESS_TRAIN_CARRIAGE_INDEX_UNIQUE_ERROR("同车次厢号已存在"),
  BUSINESS_STATION_NOT_EXIST("车站不存在"),
  BUSINESS_TRAIN_NOT_EXIST("车次不存在"),
  BUSINESS_START_END_STATION_SAME("始发站与终到站不能相同"),
  BUSINESS_CARRIAGE_NOT_EXIST("车厢不存在"),
  BUSINESS_SEAT_ALREADY_GENERATED("该车厢已生成座位，请勿重复生成"),
  BUSINESS_SLEEPER_SEAT_NOT_SUPPORTED("卧铺座位生成暂未支持"),
  BUSINESS_TRAIN_PRICE_EXIST("该车次该座位类型票价已存在"),
  BUSINESS_DAILY_TRAIN_EXIST("该车次该日期已排班"),
  BUSINESS_STATUS_INVALID("状态不合法"),
  BUSINESS_DAILY_TRAIN_NOT_EXIST("排班不存在"),
  BUSINESS_DAILY_SEAT_ALREADY_GENERATED("该排班已生成当日座位，请勿重复生成"),
  BUSINESS_STATION_INDEX_INVALID("出发站与到达站站序不合法"),
  BUSINESS_SEAT_NOT_GENERATED("车次尚未生成座位档案，请先生成座位"),
  BUSINESS_SEAT_NOT_ENOUGH("该区间余票不足"),
  BUSINESS_TRAIN_PRICE_NOT_EXIST("该车次该座位类型票价未配置"),
  BUSINESS_ORDER_NOT_EXIST("订单不存在"),
  BUSINESS_ORDER_STATUS_INVALID("订单状态不允许该操作"),
  BUSINESS_ORDER_PAY_EXPIRED("订单已超过支付时间，请重新下单"),
  BUSINESS_ORDER_LOCK_BUSY("当前下单人数过多，请稍后重试"),
  BUSINESS_MEMBER_NOT_LOGIN("未登录或登录已过期，请重新登录"),
  BUSINESS_ORDER_CONCURRENT_CONFLICT("订单状态已变更，请刷新后重试"),
  BUSINESS_ORDER_IDEMPOTENT_REPEAT("请勿重复提交订单");

  //描述
  private String desc;

  BusinessExceptionEnum(String desc) {
    this.desc = desc;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  @Override
  public String toString() {
    return "BusinessExceptionEnum{" +
        "desc='" + desc + '\'' +
        '}';
  }
}
