package com.snowdrift.framework.security.satoken.config;

import com.snowdrift.framework.security.satoken.interceptor.SecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SnowdriftSecuritySaTokenConfiguration
 * @author 83674
 * @date 2026/5/15-14:02
 * @description SaToken安全配置类
 * @since 1.0.0
 */
@Configuration
public class SnowdriftSecuritySaTokenConfiguration implements WebMvcConfigurer {

    /**
     * 添加拦截器
     *
     * @param registry registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-resources/**","/v2/api-docs/**","/doc.html","/swagger-ui.html","/error");
    }
}
