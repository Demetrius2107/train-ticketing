package com.trainticketing.business.enums;

/**
 * <p>Title: DailyTrainStatusEnum</p>
 * <p>Description: 每日车次状态枚举，与 daily_train.status 字段对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public enum DailyTrainStatusEnum {

    /**
     * 停运
     */
    STOP("0"),
    /**
     * 运行
     */
    RUN("1");

    /**
     * 状态编码
     */
    private final String code;

    DailyTrainStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 状态编码
     * @return 枚举，不存在返回 null
     */
    public static DailyTrainStatusEnum getByCode(String code) {
        for (DailyTrainStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 获取状态编码
     *
     * @return 编码
     */
    public String getCode() {
        return code;
    }
}
