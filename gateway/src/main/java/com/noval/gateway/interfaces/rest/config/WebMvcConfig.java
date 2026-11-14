package com.noval.gateway.interfaces.rest.config;

import com.noval.gateway.infrastructure.logging.TraceInterceptor;
import com.noval.gateway.interfaces.rest.auth.ApiKeyAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for authentication and CORS.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceInterceptor traceInterceptor;
    private final ApiKeyAuthInterceptor authInterceptor;

    public WebMvcConfig(TraceInterceptor traceInterceptor, ApiKeyAuthInterceptor authInterceptor) {
        this.traceInterceptor = traceInterceptor;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 链路追踪拦截器（最先执行）
        registry.addInterceptor(traceInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 认证拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                )
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
