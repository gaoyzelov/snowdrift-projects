package com.snowdrift.framework.web.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * I18nMessageSourceImpl
 *
 * @author 83674
 * @date 2026/5/9
 * @description 默认国际化消息源实现（基于配置文件）
 *              后续可扩展实现 II18nMessageSource 接口从数据库获取
 * @since 1.0.0
 */
@Slf4j
public class I18nMessageSourceImpl implements I18nMessageSource {

    private final MessageSource messageSource;

    public I18nMessageSourceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getMessage(String code, Locale locale) {
        return getMessage(code, null, locale);
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) {
        if (code == null) {
            return null;
        }
        
        Locale targetLocale = locale != null ? locale : LocaleContextHolder.getLocale();
        
        try {
            String message = messageSource.getMessage(code, args, targetLocale);
            // 如果返回的是 code 本身，说明未找到对应的国际化配置
            if (code.equals(message)) {
                log.debug("未找到国际化配置: code={}, locale={}", code, targetLocale);
            }
            return message;
        } catch (Exception e) {
            log.warn("获取国际化消息失败: code={}, locale={}", code, targetLocale, e);
            // 降级处理：返回 code 本身或默认消息
            return code;
        }
    }

    @Override
    public Map<String, String> getAllMessages(Locale locale) {
        // 默认实现返回空 Map
        // 后续可扩展：从数据库或缓存中加载所有消息
        log.debug("获取所有国际化消息: locale={}", locale);
        return new HashMap<>();
    }

    @Override
    public Locale[] getSupportedLocales() {
        // 默认支持的语言
        return new Locale[]{
                Locale.SIMPLIFIED_CHINESE,
                Locale.US,
        };
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
