package com.trainticketing.gateway.config;

import com.trainticketing.gateway.filter.AuthFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * <p>Title: RateLimitConfig</p>
 * <p>Description: 网关限流 key 解析：/business/order/** 下单链路按登录会员限流
 * （AuthFilter 已校验 JWT 并注入 X-Member-Id，订单接口必须登录，正常不会落到 IP 兜底分支）。
 * 限流参数（replenishRate/burstCapacity）在各路由 filters 的 RequestRateLimiter args 中配置。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-09
 * @since 1.0
 */
@Configuration
public class RateLimitConfig {

    /**
     * 下单限流 key：优先登录会员（m:{memberId}），异常兜底按来源 IP（ip:{addr}）
     *
     * @return KeyResolver
     */
    @Bean
    public KeyResolver memberKeyResolver() {
        return exchange -> {
            String memberId = exchange.getRequest().getHeaders().getFirst(AuthFilter.MEMBER_ID_HEADER);
            if (StringUtils.hasText(memberId)) {
                return Mono.just("m:" + memberId);
            }
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            String ip = remote != null ? remote.getAddress().getHostAddress() : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
