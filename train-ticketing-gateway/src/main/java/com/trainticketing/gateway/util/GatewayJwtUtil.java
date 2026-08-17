package com.trainticketing.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>Title: GatewayJwtUtil</p>
 * <p>Description: gateway 侧 JWT 校验工具。
 * 与 common.JwtUtil 共用 jwt.secret 配置实现对称校验。
 * gateway 为 webflux 不引 common（spring-web 冲突），故独立实现轻量校验。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
@Component
public class GatewayJwtUtil {

    public static final String CLAIM_MEMBER_ID = "memberId";

    @Value("${jwt.secret:trainticketing-default-secret-key-please-change-in-prod-32bytes}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验并解析 token
     *
     * @param token JWT 字符串
     * @return 载荷；校验失败抛 JwtException
     */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
        return jws.getPayload();
    }

    /**
     * 从 token 提取 memberId
     *
     * @param token JWT 字符串
     * @return memberId
     */
    public Long getMemberId(String token) {
        Claims claims = parse(token);
        Object id = claims.get(CLAIM_MEMBER_ID);
        if (id instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(id));
    }
}
