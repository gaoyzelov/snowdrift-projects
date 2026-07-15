package com.snowdrift.framework.web.config;

import com.snowdrift.framework.common.util.DateTimeUtil;
import com.snowdrift.framework.web.filter.HttpContextFilter;
import com.snowdrift.framework.web.filter.LogTraceFilter;
import com.snowdrift.framework.web.handler.WebExceptionHandler;
import com.snowdrift.framework.web.properties.CorsProperties;
import com.snowdrift.framework.web.properties.ResourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SnowdriftWebConfiguration
 *
 * @author gaoyzelov
 * @date 2026/5/7-18:00
 * @description web 自动配置
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({ResourceProperties.class, CorsProperties.class})
public class SnowdriftWebConfiguration implements WebMvcConfigurer {

    private final ResourceProperties resourceProperties;

    private final CorsProperties corsProperties;

    public SnowdriftWebConfiguration(ResourceProperties resourceProperties, CorsProperties corsProperties) {
        this.resourceProperties = resourceProperties;
        this.corsProperties = corsProperties;
    }

    /**
     * CORS 跨域配置
     * <p>
     * 通过 {@code snowdrift.web.cors.enabled=true} 启用，默认关闭。
     * 生产环境请显式配置允许的源、方法和请求头，避免使用通配符 *。
     * </p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsProperties.getEnabled()) {
            return;
        }
        if (CollectionUtils.isEmpty(corsProperties.getAllowedOriginPatterns())
                || CollectionUtils.isEmpty(corsProperties.getAllowedMethods())) {
            log.warn("CORS 已启用但 allowedOriginPatterns 或 allowedMethods 为空，已跳过 CORS 配置");
            return;
        }
        registry.addMapping(corsProperties.getPath())
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods(corsProperties.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(corsProperties.isAllowCredentials())
                .maxAge(corsProperties.getMaxAge());
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
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (resourceProperties == null || !resourceProperties.getEnabled() || CollectionUtils.isEmpty(resourceProperties.getMappings())) {
            return;
        }
        for (ResourceProperties.ResourceMapping mapping : resourceProperties.getMappings()) {
            registry.addResourceHandler(mapping.getPathPattern())
                    .addResourceLocations(mapping.getLocation())
                    .setCachePeriod(mapping.getUseCacheControl() ? mapping.getCachePeriod() : 0);
            log.info("注册静态资源映射: {} -> {}", mapping.getPathPattern(), mapping.getLocation());
        }
    }

    /**
     * 统一异常处理
     */
    @Bean
    public WebExceptionHandler webExceptionHandler() {
        return new WebExceptionHandler();
    }

    /**
     * HTTP 上下文过滤器
     * @return HTTP 上下文过滤器
     */
    @Bean
    public FilterRegistrationBean<HttpContextFilter> httpContextFilter() {
        FilterRegistrationBean<HttpContextFilter> httpContextFilterRegistrationBean = new FilterRegistrationBean<>(new HttpContextFilter());
        httpContextFilterRegistrationBean.addUrlPatterns("/*");
        httpContextFilterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return httpContextFilterRegistrationBean;
    }

    /**
     * 日志跟踪过滤器
     * @return 日志跟踪过滤器
     */
    @Bean
    public FilterRegistrationBean<LogTraceFilter> logTraceFilter() {
        FilterRegistrationBean<LogTraceFilter> logTraceFilterRegistrationBean = new FilterRegistrationBean<>(new LogTraceFilter());
        logTraceFilterRegistrationBean.addUrlPatterns("/*");
        logTraceFilterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return logTraceFilterRegistrationBean;
    }
}
