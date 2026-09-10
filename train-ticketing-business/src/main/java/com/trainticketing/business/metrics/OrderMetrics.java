package com.trainticketing.business.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <p>Title: OrderMetrics</p>
 * <p>Description: 订单链路业务指标（Micrometer → Prometheus → Grafana 业务面板）。
 * 与压测总览的系统层指标（JVM/HTTP/连接池）互补，回答业务问题：
 * 下单提交/拒绝分布、出票成败与耗时、关单来源、支付/退票/取消量、兜底扫描收敛量。
 * 命名统一 tt_order_* 前缀；拒绝原因 tag 取 BusinessExceptionEnum 枚举名（有限集合，无高基数风险）。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-09
 * @since 1.0
 */
@Component
public class OrderMetrics {

    private final MeterRegistry registry;

    /**
     * 出票（消费者：锁 + 行锁选座 + 落库）耗时，p95/p99 分位数
     */
    private final Timer ticketDuration;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.ticketDuration = Timer.builder("tt_order_ticket_duration")
                .description("出票耗时（锁等待+选座+落库，消费者侧）")
                .publishPercentiles(0.95, 0.99)
                .register(registry);
    }

    /**
     * 下单提交成功：mode=sync（同步下单）/ async（异步下单受理）
     */
    public void submit(String mode) {
        counter("tt_order_submit_total", "提交订单数", "mode", mode).increment();
    }

    /**
     * 下单拒绝：reason 取 BusinessExceptionEnum 枚举名（如 BUSINESS_SEAT_NOT_ENOUGH）
     */
    public void submitReject(String reason) {
        counter("tt_order_submit_reject_total", "下单拒绝数", "reason", reason).increment();
    }

    /**
     * 异步下单幂等命中（重复提交返回既有单号，未产生新订单）
     */
    public void idempotentHit() {
        counter("tt_order_idempotent_hit_total", "幂等命中数").increment();
    }

    /**
     * 出票结果：result=success（出票成功转待支付）/ fail（终态化出票失败，含兜底收敛）
     */
    public void ticket(String result) {
        counter("tt_order_ticket_total", "出票结果数", "result", result).increment();
    }

    /**
     * 出票耗时计时：供消费者包裹出票核心逻辑
     */
    public void recordTicketDuration(long millis) {
        ticketDuration.record(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * 关单：source=delay（延时消息）/ sweep（兜底扫描）
     */
    public void close(String source) {
        counter("tt_order_close_total", "关单数", "source", source).increment();
    }

    /**
     * 用户主动取消订单
     */
    public void cancel() {
        counter("tt_order_cancel_total", "取消订单数").increment();
    }

    /**
     * 支付成功
     */
    public void pay() {
        counter("tt_order_pay_total", "支付成功数").increment();
    }

    /**
     * 退票成功
     */
    public void refund() {
        counter("tt_order_refund_total", "退票成功数").increment();
    }

    /**
     * 兜底扫描每轮收敛的订单数（超时关单 + 悬挂出票中终态化）
     */
    public void sweep(int count) {
        counter("tt_order_sweep_total", "兜底扫描收敛订单数").increment(count);
    }

    private Counter counter(String name, String desc, String... tags) {
        return Counter.builder(name)
                .description(desc)
                .tags(tags)
                .register(registry);
    }
}
