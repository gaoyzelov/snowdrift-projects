package com.snowdrift.framework.web.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.lang.reflect.Field;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * I18nUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description i18n 工具黑盒测试：messageSource 未初始化时原样返回 key、委托 MessageSource、Locale 解析（blank/下划线/非法）、
 * 消息格式化（blank/无参/正常/非法模板兜底）与 initMessageSource 空校验
 * @since 1.0.0
 */
class I18nUtilTest {

    private static void setMessageSource(MessageSource messageSource) throws Exception {
        Field field = I18nUtil.class.getDeclaredField("messageSource");
        field.setAccessible(true);
        field.set(null, messageSource);
    }

    @BeforeEach
    void resetBefore() throws Exception {
        setMessageSource(null);
    }

    @AfterEach
    void resetAfter() throws Exception {
        setMessageSource(null);
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("messageSource 未初始化：getMessage 原样返回 key")
    void returnsCodeWhenMessageSourceNull() {
        assertThat(I18nUtil.getMessage("some.key")).isEqualTo("some.key");
        assertThat(I18nUtil.getMessage("some.key", Locale.US)).isEqualTo("some.key");
    }

    @Test
    @DisplayName("已初始化：getMessage(code, locale) 委托 MessageSource")
    void delegatesToMessageSource() throws Exception {
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage("k", null, Locale.US)).thenReturn("value");
        setMessageSource(messageSource);

        assertThat(I18nUtil.getMessage("k", Locale.US)).isEqualTo("value");
        verify(messageSource).getMessage("k", null, Locale.US);
    }

    @Test
    @DisplayName("带参数与指定 Locale 的 getMessage 委托 MessageSource")
    void delegatesWithArgsAndLocale() throws Exception {
        MessageSource messageSource = mock(MessageSource.class);
        Object[] args = {"a"};
        when(messageSource.getMessage("k", args, Locale.US)).thenReturn("va");
        setMessageSource(messageSource);

        assertThat(I18nUtil.getMessage("k", args, Locale.US)).isEqualTo("va");
    }

    @Test
    @DisplayName("initMessageSource(null) 抛 BizException")
    void initRejectsNull() {
        assertThatThrownBy(() -> I18nUtil.initMessageSource(null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("parseLocale：blank/非法返回默认；下划线与中划线均可解析")
    void parseLocale() {
        assertThat(I18nUtil.parseLocale(null)).isEqualTo(Locale.getDefault());
        assertThat(I18nUtil.parseLocale("  ")).isEqualTo(Locale.getDefault());
        assertThat(I18nUtil.parseLocale("###")).isEqualTo(Locale.getDefault());

        assertThat(I18nUtil.parseLocale("zh_CN").getLanguage()).isEqualTo("zh");
        assertThat(I18nUtil.parseLocale("zh_CN").getCountry()).isEqualTo("CN");
        assertThat(I18nUtil.parseLocale("zh-CN").getLanguage()).isEqualTo("zh");
        assertThat(I18nUtil.parseLocale("en").getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("getCurrentLocale 返回 LocaleContextHolder 当前值")
    void getCurrentLocale() {
        LocaleContextHolder.setLocale(Locale.FRANCE);

        assertThat(I18nUtil.getCurrentLocale()).isEqualTo(Locale.FRANCE);
    }

    @Test
    @DisplayName("formatMessage：blank 或无参原样返回")
    void formatMessagePassthrough() {
        assertThat(I18nUtil.formatMessage("", "a")).isEmpty();
        assertThat(I18nUtil.formatMessage("hello")).isEqualTo("hello");
        assertThat(I18nUtil.formatMessage("hello", (Object[]) null)).isEqualTo("hello");
    }

    @Test
    @DisplayName("formatMessage：正常占位符格式化")
    void formatMessageFormats() {
        assertThat(I18nUtil.formatMessage("Hello {0}, {1}", "A", "B")).isEqualTo("Hello A, B");
    }

    @Test
    @DisplayName("formatMessage：非法模板兜底返回原文，不抛异常")
    void formatMessageInvalidPatternFallsBack() {
        assertThat(I18nUtil.formatMessage("{", "a")).isEqualTo("{");
    }
}
