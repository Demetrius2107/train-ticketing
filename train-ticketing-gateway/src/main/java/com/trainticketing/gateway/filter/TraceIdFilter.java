package com.trainticketing.gateway.filter;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Title: TraceIdFilter</p>
 * <p>Description: 网关 traceId 生成与透传（链路观测的第一环）：
 * 优先复用上游带来的 X-Trace-Id，缺失则生成雪花 ID，写入请求头透传给下游服务
 * （下游 {@code com.trainticketing.common.filter.TraceIdFilter} 读取并进 MDC），
 * 网关自身日志同步写入 MDC（响应式线程切换下为尽力而为，跨服务串联以下游 header 为准）。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-09
 * @since 1.0
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    /**
     * 与 common.TraceIdFilter 约定一致的透传 header
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final String MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(incoming) ? incoming : IdUtil.getSnowflakeNextIdStr();
        MDC.put(MDC_KEY, traceId);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }

    @Override
    public int getOrder() {
        // 早于 AuthFilter(-100)：鉴权失败等网关日志也带 traceId
        return -200;
    }
}
