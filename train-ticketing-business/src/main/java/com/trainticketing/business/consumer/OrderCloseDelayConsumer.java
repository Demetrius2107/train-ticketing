package com.trainticketing.business.consumer;

import com.trainticketing.business.service.OrderService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <p>Title: OrderCloseDelayConsumer</p>
 * <p>Description: 延时关单消费者：下单成功（状态→待支付）后发送延时消息（默认 10 分钟，
 * 与订单支付过期时间对齐），到期触发关单检查。
 * <p>幂等语义：{@code closeOrder} 内部 CAS 待支付→已取消，用户按时支付（已变已支付）
 * 或已被兜底扫描关单的消息到达后天然空转，不删明细不回补。
 * 消息丢失由兜底扫描（每 5 分钟）兜住，双路径 CAS 收敛到同一终态。
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-06
 * @since 1.0
 */
@Component
@RocketMQMessageListener(
        topic = "${ticket.order.close-topic}",
        consumerGroup = "ticket-order-close-consumer-group")
public class OrderCloseDelayConsumer implements RocketMQListener<String> {

    private static final Logger LOG = LoggerFactory.getLogger(OrderCloseDelayConsumer.class);

    @Resource
    private OrderService orderService;

    /**
     * 延时到期关单检查，消息体为订单ID字符串
     *
     * @param orderId 订单ID
     */
    @Override
    public void onMessage(String orderId) {
        boolean closed = orderService.closeOrder(Long.valueOf(orderId));
        if (LOG.isDebugEnabled()) {
            LOG.debug("延时关单消息处理完毕 orderId={}, closed={}", orderId, closed);
        }
    }
}
