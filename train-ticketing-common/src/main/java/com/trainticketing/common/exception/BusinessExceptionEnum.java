package com.trainticketing.common.exception;

public enum BusinessExceptionEnum {
  MEMBER_MOBILE_EXIST("手机号已注册"),
  MEMBER_MOBILE_NOT_EXIST("请先获取短信验证码"),
  MEMBER_MOBILE_CODE_ERROR("短信验证码错误"),
  MEMBER_MOBILE_CODE_EXPIRED("短信验证码已过期"),

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
  BUSINESS_TRAIN_PRICE_EXIST("该车次该座位类型票价已存在");

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
