package com.trainticketing.business.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>Title: OrderMqConfig</p>
 * <p>Description: 订单 MQ 业务配置（topic 与延时等级），值来自 application.properties 平铺配置。
 * 延时等级沿用 RocketMQ 4.x 固定 18 级：默认 14=10min，与订单支付过期时间（下单后 10 分钟）对齐；
 * 本地演示/测试可临时改为 5=1min 快速观察关单效果。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-06
 * @since 1.0
 */
@Component
public class OrderMqConfig {

    /**
     * 出票消息 topic（异步下单生产者发送、消费者订阅）
     */
    @Value("${ticket.order.create-topic}")
    private String createTopic;

    /**
     * 延时关单 topic（下单成功后发延时消息，到期触发关单检查）
     */
    @Value("${ticket.order.close-topic}")
    private String closeTopic;

    /**
     * 关单延时等级（RocketMQ 4.x：14=10min 5=1min）
     */
    @Value("${ticket.order.close-delay-level}")
    private int closeDelayLevel;

    public String getCreateTopic() {
        return createTopic;
    }

    public String getCloseTopic() {
        return closeTopic;
    }

    public int getCloseDelayLevel() {
        return closeDelayLevel;
    }
}
