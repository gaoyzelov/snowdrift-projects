package com.snowdrift.framework.web.util;

import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * I18nUtil
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 国际化工具类
 * @since 1.0.0
 */
@Slf4j
public final class I18nUtil {

    private static volatile MessageSource messageSource;

    private I18nUtil() {
    }

    /**
     * 初始化消息源（由配置类调用）
     *
     * @param messageSource 消息源
     */
    public static void initMessageSource(MessageSource messageSource) {
        AssertUtil.notNull(messageSource, "消息源不能为空");
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 获取当前语言环境
     *
     * @return Locale
     */
    public static Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * 获取国际化消息
     *
     * @param code 消息键
     * @return 消息内容
     */
    public static String getMessage(String code) {
        return getMessage(code, getCurrentLocale());
    }

    /**
     * 获取国际化消息
     *
     * @param code   消息键
     * @param locale 语言环境
     * @return 消息内容
     */
    public static String getMessage(String code, Locale locale) {
        return getMessage(code, null, locale);
    }

    /**
     * 获取国际化消息（带参数）
     *
     * @param code 消息键
     * @param args 参数
     * @return 消息内容
     */
    public static String getMessage(String code, Object... args) {
        return getMessage(code, args, getCurrentLocale());
    }

    /**
     * 获取国际化消息（带参数）
     *
     * @param code   消息键
     * @param args   参数
     * @param locale 语言环境
     * @return 消息内容
     */
    public static String getMessage(String code, Object[] args, Locale locale) {
        if (messageSource == null) {
            log.warn("消息源未初始化");
            return code;
        }
        return messageSource.getMessage(code, args, locale);
    }

    /**
     * 解析语言字符串为 Locale 对象
     *
     * @param language 语言字符串（如：zh_CN, en_US, zh-CN）
     * @return Locale 对象
     */
    public static Locale parseLocale(String language) {
        // 处理格式
        Locale locale = Locale.getDefault();
        if (StringUtils.isBlank(language)) {
            return locale;
        }
        try {
            if (language.contains(StrConst.UNDERLINE)) {
                // 下划线
                String[] arr = language.split(StrConst.UNDERLINE);
                locale = new Locale(arr[0], arr[1]);
            } else if (language.contains(StrConst.MIDLINE)) {
                // 中划线
                String[] arr = language.split(StrConst.MIDLINE);
                locale = new Locale(arr[0], arr[1]);
            } else {
                locale = new Locale(language);
            }
        } catch (RuntimeException e) {
            log.error("语言字符串格式错误：{}", language, e);
        }
        return locale;
    }

    /**
     * 格式化消息（提供公共工具方法）
     *
     * @param message 消息模板
     * @param args    参数
     * @return 格式化后的消息
     */
    public static String formatMessage(String message, Object... args) {
        if (message == null || args == null || args.length == 0) {
            return message;
        }
        try {
            return MessageFormat.format(message, args);
        } catch (Exception e) {
            log.error("格式化消息失败: message={}", message, e);
            return message;
        }
    }
}
