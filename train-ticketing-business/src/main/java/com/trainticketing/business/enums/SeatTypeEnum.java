package com.trainticketing.business.enums;

/**
 * <p>Title: SeatTypeEnum</p>
 * <p>Description: 座位类型枚举，与 train_carriage.seat_type / train_seat.seat_type 字段对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public enum SeatTypeEnum {

    /** 商务座 */
    BUSINESS("1"),
    /** 一等座 */
    FIRST_CLASS("2"),
    /** 二等座 */
    SECOND_CLASS("3"),
    /** 硬卧 */
    HARD_SLEEPER("4"),
    /** 软卧 */
    SOFT_SLEEPER("5");

    /** 座位类型编码 */
    private final String code;

    SeatTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 获取座位类型编码
     *
     * @return 编码
     */
    public String getCode() {
        return code;
    }
}
