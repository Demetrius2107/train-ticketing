package com.trainticketing.member.enums;

/**
 * <p>Title: PassengerTypeEnum</p>
 * <p>Description: 旅客类型枚举，与 passenger.type 字段对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public enum PassengerTypeEnum {

    /** 成人 */
    ADULT("1"),
    /** 儿童 */
    CHILD("2"),
    /** 学生 */
    STUDENT("3");

    /** 类型编码 */
    private final String code;

    PassengerTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 类型编码
     * @return 枚举，不存在返回 null
     */
    public static PassengerTypeEnum getByCode(String code) {
        for (PassengerTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取类型编码
     *
     * @return 编码
     */
    public String getCode() {
        return code;
    }
}
