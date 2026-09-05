package com.trainticketing.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Title: JacksonConfig</p>
 * <p>Description: 全局 JSON 序列化配置：Long 一律序列化为字符串。
 * 雪花 ID 为 19 位，超出 JS Number.MAX_SAFE_INTEGER（2^53，约 16 位），
 * 前端 / HTTP 客户端把响应 JSON 解析成数字时会精度丢失
 * （如 2096232709454139424 被舍入为 2.0962327094541394E18，回传后无法解析）。
 * 统一以字符串下发，前端按字符串处理 ID——业界通行做法。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-05
 * @since 1.0
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
