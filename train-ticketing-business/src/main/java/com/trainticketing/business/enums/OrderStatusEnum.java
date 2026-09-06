package com.trainticketing.business.enums;

/**
 * <p>Title: OrderStatusEnum</p>
 * <p>Description: 订单状态枚举，与 train_order.status 字段对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public enum OrderStatusEnum {

    /**
     * 待支付
     */
    PENDING("0"),
    /**
     * 已支付
     */
    PAID("1"),
    /**
     * 已取消
     */
    CANCELLED("2"),
    /**
     * 已退票
     */
    REFUNDED("3"),
    /**
     * 出票中（异步下单排队：订单已受理，等待消费者选座出票）
     */
    QUEUING("4"),
    /**
     * 出票失败（异步下单出票失败或消息重试耗尽，余票已回补）
     */
    FAILED("5");

    /**
     * 状态编码
     */
    private final String code;

    OrderStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 状态编码
     * @return 枚举，不存在返回 null
     */
    public static OrderStatusEnum getByCode(String code) {
        for (OrderStatusEnum status : values()) {
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
