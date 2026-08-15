package com.trainticketing.business.enums;

/**
 * <p>Title: TrainTypeEnum</p>
 * <p>Description: 车次类型枚举，与 train.type 字段对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public enum TrainTypeEnum {

    /** 高铁 */
    HIGH_SPEED("1"),
    /** 动车 */
    EMU("2"),
    /** 特快 */
    EXPRESS("3"),
    /** 普快 */
    NORMAL("4");

    /** 类型编码 */
    private final String code;

    TrainTypeEnum(String code) {
        this.code = code;
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
