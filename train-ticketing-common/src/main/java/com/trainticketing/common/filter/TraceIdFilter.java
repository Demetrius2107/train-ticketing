package com.trainticketing.common.filter;

import cn.hutool.core.util.IdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <p>Title: TraceIdFilter</p>
 * <p>Description: traceId 链路标识 filter（member/business 等 Servlet 应用共用）。
 * 优先复用网关透传的 X-Trace-Id（一次请求在网关生成、跨服务传播），缺失（直连调试）时自生成；
 * 写入 MDC 供日志输出（需配合 logging.pattern.level 加 %X{traceId}），并回写响应头，
 * 便于前端/压测脚本按 traceId 检索整条请求日志。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-09
 * @since 1.0
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    /**
     * 网关与下游服务间传递 traceId 的 header 名
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC key，日志 pattern 通过 %X{traceId} 引用
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = IdUtil.getSnowflakeNextIdStr();
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        // 最早执行：先于 LogAspect 等业务逻辑，保证入口日志就带 traceId
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
