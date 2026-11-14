package com.noval.gateway.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTP 请求链路追踪拦截器
 * 为每个请求生成 traceId 并记录请求日志
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TraceInterceptor.class);

    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 初始化链路追踪
        TraceContext.init();

        // 记录请求开始
        String traceId = TraceContext.getTraceId();
        request.setAttribute(START_TIME, System.currentTimeMillis());

        StringBuilder sb = new StringBuilder();
        sb.append("[Request] ");
        sb.append("method=").append(request.getMethod());
        sb.append(", uri=").append(request.getRequestURI());
        sb.append(", remoteAddr=").append(request.getRemoteAddr());
        sb.append(", userAgent=").append(request.getHeader("User-Agent"));

        log.info(sb.toString());

        // 添加 traceId 到响应头
        response.setHeader("X-Trace-Id", traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("[Response] ");
        sb.append("method=").append(request.getMethod());
        sb.append(", uri=").append(request.getRequestURI());
        sb.append(", status=").append(response.getStatus());
        sb.append(", duration=").append(duration).append("ms");

        if (ex != null) {
            sb.append(", error=").append(ex.getMessage());
            log.error(sb.toString());
        } else {
            log.info(sb.toString());
        }

        // 清理上下文
        TraceContext.clear();
    }
}
