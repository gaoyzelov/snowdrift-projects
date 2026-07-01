package com.snowdrift.framework.web.i18n;

import java.util.Locale;
import java.util.Map;

/**
 * II18nMessageSource
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 国际化消息源接口（可扩展为从数据库获取）
 * @since 1.0.0
 */
public interface I18nMessageSource {

    /**
     * 获取国际化消息
     *
     * @param code   消息键
     * @param locale 语言环境
     * @return 消息内容
     */
    String getMessage(String code, Locale locale);

    /**
     * 获取国际化消息（带参数）
     *
     * @param code   消息键
     * @param args   参数
     * @param locale 语言环境
     * @return 消息内容
     */
    String getMessage(String code, Object[] args, Locale locale);

    /**
     * 获取指定语言的所有消息
     *
     * @param locale 语言环境
     * @return 消息 Map
     */
    Map<String, String> getAllMessages(Locale locale);

    /**
     * 获取支持的语言列表
     *
     * @return 语言列表
     */
    Locale[] getSupportedLocales();
}
