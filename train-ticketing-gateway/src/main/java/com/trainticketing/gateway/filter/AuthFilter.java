package com.trainticketing.gateway.filter;

import com.trainticketing.gateway.util.GatewayJwtUtil;
import io.jsonwebtoken.Claims;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Title: AuthFilter</p>
 * <p>Description: 网关全局鉴权 filter。
 * 校验 Authorization: Bearer {token}，通过后将 memberId 注入下游 header X-Member-Id。
 * 白名单（公开接口）：登录/注册/发验证码、车次/余票/车站/车厢/座位/票价/排班查询、对账运维接口。
 * 订单接口（/business/order/**）必须登录。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 * @since 1.0
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(AuthFilter.class);

    /**
     * 注入下游的会员ID header，business 侧从该 header 取 memberId
     */
    public static final String MEMBER_ID_HEADER = "X-Member-Id";

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * 白名单：公开接口放行（登录/注册/验证码、查询类、运维对账）
     */
    private static final List<String> WHITELIST = List.of(
            // member 公开
            "/member/member/register",
            "/member/member/send-code",
            "/member/member/login",
            "/member/member/count",
            "/member/hello",
            // business 查询类公开
            "/business/ticket/**",
            "/business/station/**",
            "/business/train/**",
            "/business/daily-train/**",
            "/business/reconcile/**",
            "/business/hello"
    );

    private final GatewayJwtUtil jwtUtil;

    public AuthFilter(GatewayJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (WHITELIST.stream().anyMatch(p -> matcher.match(p, path))) {
            return chain.filter(exchange);
        }

        // 校验 Authorization
        String auth = request.getHeaders().getFirst(AUTH_HEADER);
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            LOG.warn("鉴权失败：缺少 Authorization header, path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String token = auth.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtUtil.parse(token);
            Long memberId = extractMemberId(claims);
            // 注入 memberId 到下游 header，business 侧从 X-Member-Id 取
            ServerHttpRequest mutated = request.mutate()
                    .header(MEMBER_ID_HEADER, String.valueOf(memberId))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            LOG.warn("鉴权失败：token 无效, path={}, error={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Long extractMemberId(Claims claims) {
        Object id = claims.get(GatewayJwtUtil.CLAIM_MEMBER_ID);
        if (id instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(id));
    }

    @Override
    public int getOrder() {
        // 早于路由转发执行
        return -100;
    }
}
