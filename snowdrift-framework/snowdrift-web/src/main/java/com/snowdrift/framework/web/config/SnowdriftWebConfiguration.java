package com.snowdrift.framework.web.config;

import com.snowdrift.framework.common.util.DateTimeUtil;
import com.snowdrift.framework.web.handler.WebExceptionHandler;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SnowdriftWebConfiguration
 *
 * @author 83674
 * @date 2026/5/7-18:00
 * @description web 自动配置
 * @since 1.0.0
 */
@Configuration
@ServletComponentScan(basePackages = "com.snowdrift.framework.web.filter")
public class SnowdriftWebConfiguration implements WebMvcConfigurer {

    /**
     * CORS 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(CorsConfiguration.ALL)
                .allowedMethods(CorsConfiguration.ALL)
                .allowedHeaders(CorsConfiguration.ALL)
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 日期时间格式化
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 日期时间格式化
        DateTimeFormatterRegistrar dateTimeRegistrar = new DateTimeFormatterRegistrar();
        dateTimeRegistrar.setDateFormatter(DateTimeUtil.DATE_FORMATTER);
        dateTimeRegistrar.setTimeFormatter(DateTimeUtil.TIME_FORMATTER);
        dateTimeRegistrar.setDateTimeFormatter(DateTimeUtil.DATETIME_FORMATTER);
        dateTimeRegistrar.registerFormatters(registry);
    }

    /**
     * 统一异常处理
     */
    @Bean
    public WebExceptionHandler webExceptionHandler() {
        return new WebExceptionHandler();
    }
}
