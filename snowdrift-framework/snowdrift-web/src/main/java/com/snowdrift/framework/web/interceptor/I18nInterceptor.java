package com.snowdrift.framework.web.interceptor;

import com.snowdrift.framework.web.i18n.I18nUtil;
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
 *
 * @author 83674
 * @date 2026/5/9
 * @description 国际化语言拦截器
 * 优先级：URL 参数（默认 lang） > 默认语言
 * 通过 LocaleContextHolder 设置当前请求的语言环境，在 afterCompletion 中清理
 * @since 1.0.0
 */
@Slf4j
public class I18nInterceptor implements HandlerInterceptor {

    private final I18nProperties i18nProperties;

    public I18nInterceptor(I18nProperties i18nProperties) {
        this.i18nProperties = i18nProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从参数获取
        String lang = request.getParameter(i18nProperties.getParamName());

        // 2. 如果没有，从请求头获取
        if (StringUtils.isBlank(lang)) {
            // 检查 Accept-Language 头部
            Locale requestLocale = request.getLocale();
            if (requestLocale != null && i18nProperties.getSupportedLocales()
                    .contains(requestLocale.toString())) {
                lang = requestLocale.toString();
            } else {
                // 回退到配置的默认值
                lang = i18nProperties.getDefaultLocale();
            }
        }

        // 3. 验证是否支持该语言
        Locale locale = I18nUtil.parseLocale(lang);
        if (isSupported(locale, i18nProperties.getSupportedLocales())) {
            LocaleContextHolder.setLocale(locale);
            log.debug("设置语言环境: {}", locale);
        } else if (isSupported(request.getLocale(), i18nProperties.getSupportedLocales())) {
            LocaleContextHolder.setLocale(request.getLocale());
        } else {
            log.warn("不支持的语言环境: {}，使用默认语言", lang);
            LocaleContextHolder.setLocale(I18nUtil.parseLocale(i18nProperties.getDefaultLocale()));
        }

        return true;
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
