package com.trainticketing.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>Title: JwtUtil</p>
 * <p>Description: JWT 签发/校验工具（member 签发用）。
 * 载荷含 memberId、mobile；HS256 对称签名，密钥由 jwt.secret 配置。
 * gateway 侧校验用独立轻量实现（避免引入 common 的 spring-web 与 webflux 冲突），
 * 双方共用同一密钥即可。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
@Component
public class JwtUtil {

    /** 载荷中 memberId 的 claim key */
    public static final String CLAIM_MEMBER_ID = "memberId";

    /** 载荷中 mobile 的 claim key */
    public static final String CLAIM_MOBILE = "mobile";

    @Value("${jwt.secret:trainticketing-default-secret-key-please-change-in-prod-32bytes}")
    private String secret;

    @Value("${jwt.expire-hours:2}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // HS256 要求密钥 >= 256 bit (32 字节)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT
     *
     * @param memberId 会员ID
     * @param mobile   手机号
     * @return token 字符串
     */
    public String generate(Long memberId, String mobile) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
            .subject(String.valueOf(memberId))
            .claim(CLAIM_MEMBER_ID, memberId)
            .claim(CLAIM_MOBILE, mobile)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    /**
     * 校验并解析 token，失败抛 JwtException
     *
     * @param token JWT 字符串
     * @return 载荷
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
