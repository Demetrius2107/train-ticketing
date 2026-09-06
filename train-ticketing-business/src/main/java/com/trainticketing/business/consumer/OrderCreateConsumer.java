package com.trainticketing.business.consumer;

import com.trainticketing.business.message.OrderCreateMessage;
import com.trainticketing.business.service.OrderService;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * <p>Title: OrderCreateConsumer</p>
 * <p>Description: 出票消息消费者（异步下单削峰的落库侧）。
 * <p>并发语义：并发消费（默认 20 消费线程），防超卖正确性不依赖消费串行——
 * 仍由 Redisson 锁（dailyTrainId:seatType 粒度）串行化选座 + DB 行锁兜底，与同步链路同源。
 * <p>失败分型（可靠性核心）：
 * 1. 余票耗尽（确定性失败）→ 置出票失败 + 回补预扣余票，ACK 不重试；
 * 2. 并发冲突（兜底扫描已接管该单）→ ACK 放弃，幂等空转；
 * 3. 锁忙/DB 抖动等临时失败 → 抛出走 RocketMQ 递增间隔重试（{@code maxReconsumeTimes} 次后进死信）；
 * 4. 重试耗尽/消息丢失 → 订单悬挂为出票中，由 {@code OrderService.sweepTimeoutOrders} 兜底收敛。
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-06
 * @since 1.0
 */
@Component
@RocketMQMessageListener(
        topic = "${ticket.order.create-topic}",
        consumerGroup = "ticket-order-create-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadNumber = 20,
        maxReconsumeTimes = 5)
public class OrderCreateConsumer implements RocketMQListener<OrderCreateMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(OrderCreateConsumer.class);

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(OrderCreateMessage message) {
        try {
            orderService.processAsyncOrder(message);
        } catch (BusinessException e) {
            if (BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH == e.getE()) {
                // 确定性失败：余票耗尽（缓存-DB 漂移或座位真已售罄），终态化 + 回补，不重试
                boolean handled = orderService.failQueuingOrder(message.getOrderId(), message.getDailyTrainId(),
                        message.getSeatType(), message.getDepartIndex(), message.getArriveIndex(),
                        message.getPassengers().size());
                LOG.info("出票失败（余票耗尽）已终态化 orderNo={}, 本次处理={}", message.getOrderNo(), handled);
                return;
            }
            if (BusinessExceptionEnum.BUSINESS_ORDER_CONCURRENT_CONFLICT == e.getE()) {
                // 订单状态已被兜底扫描接管（出票中→出票失败），本次事务已回滚，ACK 放弃
                LOG.info("出票放弃（订单已被兜底扫描终态化） orderNo={}", message.getOrderNo());
                return;
            }
            // 锁忙等临时失败：抛出走 RocketMQ 递增间隔重试
            throw new RuntimeException("出票临时失败，等待重试 orderNo=" + message.getOrderNo()
                    + ", 原因=" + e.getE().getDesc(), e);
        } catch (Exception e) {
            // 系统异常：同样走重试
            throw new RuntimeException("出票系统异常 orderNo=" + message.getOrderNo(), e);
        }
    }
}
