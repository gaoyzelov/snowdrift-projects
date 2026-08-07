package com.snowdrift.framework.web.config;

import com.snowdrift.framework.web.advice.ResultI18nAdvice;
import com.snowdrift.framework.web.util.I18nUtil;
import com.snowdrift.framework.web.interceptor.I18nInterceptor;
import com.snowdrift.framework.web.properties.I18nProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * I18nConfiguration
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 国际化配置类
 * @since 1.0.0
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnProperty(prefix = "snowdrift.i18n", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(I18nProperties.class)
public class SnowdriftI18nConfiguration implements WebMvcConfigurer {

    private final I18nProperties properties;

    public SnowdriftI18nConfiguration(I18nProperties i18nProperties) {
        this.properties = i18nProperties;
    }

    /**
     * 配置 MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames(properties.getBaseNames().stream()
                .map(name -> name.startsWith("classpath:") ? name : "classpath:" + name)
                .toArray(String[]::new));
        messageSource.setDefaultEncoding(properties.getEncoding());
        messageSource.setUseCodeAsDefaultMessage(properties.getUseCodeAsDefaultMessage());
        // 缓存配置：-1 表示永不缓存（开发环境），正数表示缓存秒数（生产环境）
        messageSource.setCacheSeconds(properties.getCacheSeconds());
        messageSource.setDefaultLocale(I18nUtil.parseLocale(properties.getDefaultLocale()));
        // 初始化工具类
        I18nUtil.initMessageSource(messageSource);
        return messageSource;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new I18nInterceptor(properties));
    }

    /**
     * Result 响应体 i18n 自动解析
     */
    @Bean
    public ResultI18nAdvice resultI18nAdvice() {
        return new ResultI18nAdvice();
    }
}
