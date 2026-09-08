package com.snowdrift.framework.web.interceptor;

import com.snowdrift.framework.web.util.I18nUtil;
import com.snowdrift.framework.web.properties.I18nProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * I18nInterceptor
 * 优先级：URL 参数（默认 lang） > Accept-Language 请求头 > 默认语言
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 国际化语言拦截器
 * @since 1.0.0
 */
@Slf4j
public class I18nInterceptor implements HandlerInterceptor {

    private final I18nProperties properties;

    public I18nInterceptor(I18nProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从参数获取
        String lang = request.getParameter(properties.getParamName());

        // 2. 参数缺失时从 Accept-Language 请求头解析（支持语言子标签匹配，如 en -> en_US）
        if (StringUtils.isBlank(lang)) {
            lang = resolveFromAcceptLanguage(request);
        }

        // 3. 仍未解析到则回退到配置的默认语言
        if (StringUtils.isBlank(lang)) {
            lang = properties.getDefaultLocale();
        }

        // 4. 验证并设置语言环境
        Locale locale = I18nUtil.parseLocale(lang);
        if (isSupported(locale, properties.getSupportedLocales())) {
            LocaleContextHolder.setLocale(locale);
            log.debug("设置语言环境: {}", locale);
        } else {
            log.warn("不支持的语言环境: {}，使用默认语言", lang);
            LocaleContextHolder.setLocale(I18nUtil.parseLocale(properties.getDefaultLocale()));
        }

        return true;
    }

    /**
     * 从 Accept-Language 请求头解析支持的语言标签。
     * <p>
     * 无请求头时返回 {@code null}（由调用方回退默认语言）；有请求头时优先精确匹配支持列表，
     * 其次匹配语言子标签（如 {@code en} -> {@code en_US}），都未命中时返回 {@code null}。
     * </p>
     *
     * @param request 当前请求
     * @return 匹配到的支持语言标签；未命中返回 {@code null}
     */
    private String resolveFromAcceptLanguage(HttpServletRequest request) {
        if (StringUtils.isBlank(request.getHeader("Accept-Language"))) {
            return null;
        }
        Locale requestLocale = request.getLocale();
        if (requestLocale == null) {
            return null;
        }
        return matchSupportedLocale(requestLocale, properties.getSupportedLocales());
    }

    /**
     * 在支持的语言列表中匹配最接近请求语言的语言标签：优先精确匹配，其次语言子标签匹配。
     *
     * @param requestLocale    请求语言
     * @param supportedLocales 支持的语言列表
     * @return 匹配到的支持语言标签；未命中返回 {@code null}
     */
    private String matchSupportedLocale(Locale requestLocale, List<String> supportedLocales) {
        // 1. 精确匹配（如 zh_CN / en_US）
        String requestTag = requestLocale.toString();
        if (supportedLocales.contains(requestTag)) {
            return requestTag;
        }
        // 2. 语言子标签匹配（如请求 en，支持列表含 en_US -> en_US）
        String requestLanguage = requestLocale.getLanguage();
        if (StringUtils.isNotBlank(requestLanguage)) {
            for (String supported : supportedLocales) {
                if (supported != null && requestLanguage.equals(I18nUtil.parseLocale(supported).getLanguage())) {
                    return supported;
                }
            }
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 LocaleContext，防止内存泄漏
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * 判断是否支持该语言
     *
     * @param locale           语言环境
     * @param supportedLocales 支持的语言列表
     * @return 是否支持
     */
    private boolean isSupported(Locale locale, List<String> supportedLocales) {
        String localeStr = locale.toString();
        return supportedLocales.contains(localeStr) ||
                supportedLocales.contains(locale.getLanguage());
    }
}
