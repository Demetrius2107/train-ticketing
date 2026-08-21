package com.trainticketing.business.enums;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Title: SeatTypeEnum</p>
 * <p>Description: 座位类型枚举，与 train_carriage.seat_type / train_seat.seat_type 字段对应；
 * 内置每排座位布局规则（业务规则内聚于枚举，避免魔法值散落，座位生成时直接引用）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public enum SeatTypeEnum {

    /**
     * 商务座：每排3座 A C F
     */
    BUSINESS("1", 3, Arrays.asList("A", "C", "F"), false),
    /**
     * 一等座：每排4座 A C D F
     */
    FIRST_CLASS("2", 4, Arrays.asList("A", "C", "D", "F"), false),
    /**
     * 二等座：每排5座 A B C D F
     */
    SECOND_CLASS("3", 5, Arrays.asList("A", "B", "C", "D", "F"), false),
    /**
     * 硬卧：每单元上中下三铺，铺位标签序越大越靠下（下铺优先分配）
     */
    HARD_SLEEPER("4", 3, Arrays.asList("U", "M", "D"), true),
    /**
     * 软卧：每包厢上下两铺
     */
    SOFT_SLEEPER("5", 2, Arrays.asList("U", "D"), true);

    /**
     * 座位类型编码
     */
    private final String code;

    /**
     * 每排/每单元座位数
     */
    private final int seatsPerRow;

    /**
     * 每排/每单元座位标签序列
     */
    private final List<String> seatLabels;

    /**
     * 是否卧铺（铺位布局，选座时按下铺优先）
     */
    private final boolean sleeper;

    SeatTypeEnum(String code, int seatsPerRow, List<String> seatLabels, boolean sleeper) {
        this.code = code;
        this.seatsPerRow = seatsPerRow;
        this.seatLabels = seatLabels;
        this.sleeper = sleeper;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 座位类型编码
     * @return 枚举，不存在返回 null
     */
    public static SeatTypeEnum getByCode(String code) {
        for (SeatTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取座位类型编码
     *
     * @return 编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取每排座位数
     *
     * @return 每排座位数
     */
    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    /**
     * 获取每排座位标签序列
     *
     * @return 座位标签列表
     */
    public List<String> getSeatLabels() {
        return seatLabels;
    }

    /**
     * 是否卧铺
     *
     * @return true 为卧铺
     */
    public boolean isSleeper() {
        return sleeper;
    }
}
