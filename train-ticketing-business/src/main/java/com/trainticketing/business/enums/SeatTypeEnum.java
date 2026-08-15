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
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public enum SeatTypeEnum {

    /** 商务座：每排3座 A C F */
    BUSINESS("1", 3, Arrays.asList("A", "C", "F")),
    /** 一等座：每排4座 A C D F */
    FIRST_CLASS("2", 4, Arrays.asList("A", "C", "D", "F")),
    /** 二等座：每排5座 A B C D F */
    SECOND_CLASS("3", 5, Arrays.asList("A", "B", "C", "D", "F")),
    /** 硬卧：铺位布局，阶段1 暂不支持按排生成 */
    HARD_SLEEPER("4", 0, null),
    /** 软卧：铺位布局，阶段1 暂不支持按排生成 */
    SOFT_SLEEPER("5", 0, null);

    /** 座位类型编码 */
    private final String code;

    /** 每排座位数（卧铺为0，表示不支持按排生成） */
    private final int seatsPerRow;

    /** 每排座位标签序列（卧铺为null） */
    private final List<String> seatLabels;

    SeatTypeEnum(String code, int seatsPerRow, List<String> seatLabels) {
        this.code = code;
        this.seatsPerRow = seatsPerRow;
        this.seatLabels = seatLabels;
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
}
