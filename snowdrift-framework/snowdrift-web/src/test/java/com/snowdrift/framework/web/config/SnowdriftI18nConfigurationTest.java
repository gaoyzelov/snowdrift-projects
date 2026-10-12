package com.snowdrift.framework.web.config;

import com.snowdrift.framework.web.interceptor.I18nInterceptor;
import com.snowdrift.framework.web.properties.I18nProperties;
import com.snowdrift.framework.web.util.I18nUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SnowdriftI18nConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description i18n 自动配置黑盒测试：basename 自动补 classpath: 前缀并解析到测试资源、MessageSource 装配、
 * 初始化 I18nUtil、注册 I18nInterceptor
 * @since 1.0.0
 */
class SnowdriftI18nConfigurationTest {

    private I18nProperties props() {
        I18nProperties properties = new I18nProperties();
        properties.setBaseNames(List.of("i18n/test-messages"));
        properties.setEncoding("UTF-8");
        properties.setUseCodeAsDefaultMessage(true);
        properties.setCacheSeconds(3600);
        properties.setDefaultLocale("en_US");
        return properties;
    }

    @AfterEach
    void resetI18nUtil() throws Exception {
        Field field = I18nUtil.class.getDeclaredField("messageSource");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    @DisplayName("messageSource：basename 自动补 classpath: 前缀并解析到资源")
    void messageSourceResolvesBasename() {
        SnowdriftI18nConfiguration configuration = new SnowdriftI18nConfiguration(props());

        MessageSource messageSource = configuration.messageSource();

        assertThat(messageSource).isInstanceOf(ReloadableResourceBundleMessageSource.class);
        assertThat(messageSource.getMessage("greeting", null, Locale.ENGLISH)).isEqualTo("Hello");
        // 注意：绑定 I18nUtil 不再是本方法的副作用（宿主提供 MessageSource 时本方法根本不会执行），
        // 已改由 i18nUtilMessageSourceBinder 在启动完成后统一绑定，
        // 见 SnowdriftI18nMessageSourceBindingTest。
    }

    @Test
    @DisplayName("useCodeAsDefaultMessage=true：缺失 key 原样返回 code")
    void missingKeyReturnsCode() {
        MessageSource messageSource = new SnowdriftI18nConfiguration(props()).messageSource();

        assertThat(messageSource.getMessage("no.such.key", null, Locale.ENGLISH)).isEqualTo("no.such.key");
    }

    @Test
    @DisplayName("addInterceptors：注册 I18nInterceptor")
    void registersInterceptor() {
        SnowdriftI18nConfiguration configuration = new SnowdriftI18nConfiguration(props());
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        configuration.addInterceptors(registry);

        ArgumentCaptor<HandlerInterceptor> captor = ArgumentCaptor.forClass(HandlerInterceptor.class);
        verify(registry).addInterceptor(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(I18nInterceptor.class);
    }
}
