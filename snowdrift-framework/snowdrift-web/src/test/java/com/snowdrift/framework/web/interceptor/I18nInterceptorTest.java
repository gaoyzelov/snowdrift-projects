package com.snowdrift.framework.web.interceptor;

import com.snowdrift.framework.web.properties.I18nProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I18nInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description i18n 拦截器黑盒测试：语言解析优先级（URL 参数 > Accept-Language > 默认）、支持列表精确/子标签匹配、
 * afterCompletion 重置 LocaleContext
 * @since 1.0.0
 */
class I18nInterceptorTest {

    private I18nProperties props(List<String> supportedLocales) {
        I18nProperties properties = new I18nProperties();
        properties.setParamName("lang");
        properties.setDefaultLocale("zh_CN");
        properties.setSupportedLocales(supportedLocales);
        return properties;
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("URL 参数优先：lang=en_US 时设置为 en_US（忽略 Accept-Language）")
    void urlParamTakesPriority() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("zh_CN", "en_US")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("lang", "en_US");
        request.addHeader("Accept-Language", "zh-CN");
        request.setPreferredLocales(List.of(Locale.CHINA));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(LocaleContextHolder.getLocale().getLanguage()).isEqualTo("en");
        assertThat(LocaleContextHolder.getLocale().getCountry()).isEqualTo("US");
    }

    @Test
    @DisplayName("无参数时按 Accept-Language 精确匹配支持列表")
    void acceptLanguageExactMatch() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("zh_CN", "en_US")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        request.setPreferredLocales(List.of(Locale.US));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.US);
    }

    @Test
    @DisplayName("Accept-Language 子标签匹配：请求 en，支持列表含 en_US → en_US")
    void acceptLanguageSubtagMatch() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("en_US")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en");
        request.setPreferredLocales(List.of(Locale.ENGLISH));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.US);
    }

    @Test
    @DisplayName("无参数且无 Accept-Language 头：回退默认语言 zh_CN")
    void fallsBackToDefault() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("zh_CN", "en_US")));
        MockHttpServletRequest request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.CHINA);
    }

    @Test
    @DisplayName("Accept-Language 未命中支持列表：回退默认语言")
    void unmatchedAcceptLanguageFallsBack() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("zh_CN")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr-FR");
        request.setPreferredLocales(List.of(Locale.FRANCE));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.CHINA);
    }

    @Test
    @DisplayName("preHandle 恒返回 true（不阻断请求）")
    void preHandleReturnsTrue() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of()));

        boolean result = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("afterCompletion 重置 LocaleContext（防线程复用串语言）")
    void afterCompletionResets() {
        I18nInterceptor interceptor = new I18nInterceptor(props(List.of("en_US")));
        LocaleContextHolder.setLocale(Locale.US);

        interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), null);

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    }
}
