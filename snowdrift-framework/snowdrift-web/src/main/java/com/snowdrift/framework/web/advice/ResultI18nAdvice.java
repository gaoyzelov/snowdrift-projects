package com.snowdrift.framework.web.advice;

import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.web.i18n.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * ResultI18nAdvice
 * <p>
 * 在 {@link Result} 响应序列化之前，自动将 {@code msg} 字段中的 i18n key
 * 解析为当前 Locale 对应的国际化文本。
 * </p>
 * <p>
 * 例如：{@code Result.ok()} 产生的 {@code msg="common.success"} 会被解析为
 * {@code "操作成功"}（zh_CN）或 {@code "Operation successful"}（en_US）。
 * </p>
 * <p>
 * 已通过 {@link com.snowdrift.framework.web.handler.WebExceptionHandler} 等
 * 异常处理器解析过的消息（不含 "." 的自然语言文本）不会被二次处理。
 * </p>
 *
 * @author 83674
 * @date 2026/6/9
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class ResultI18nAdvice implements ResponseBodyAdvice<Result<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 仅处理返回类型为 Result 的响应
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body,
                                     MethodParameter returnType,
                                     MediaType selectedContentType,
                                     Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     ServerHttpRequest request,
                                     ServerHttpResponse response) {
        if (body == null || StringUtils.isBlank(body.getMsg())) {
            return body;
        }

        String msg = body.getMsg();

        // 仅当 msg 看起来是 i18n key 时才解析（包含 "." 且不含空格）
        // 已通过异常处理器解析的自然语言文本（如 "操作成功"）不含 "."，会被跳过
        if (isI18nKey(msg)) {
            String resolved = I18nUtil.getMessage(msg);
            if (!msg.equals(resolved)) {
                log.debug("Result msg i18n 解析: {} -> {}", msg, resolved);
                body.setMsg(resolved);
            }
        }

        return body;
    }

    /**
     * 判断字符串是否为 i18n key。
     * <p>
     * i18n key 的约定格式：由字母、数字、"."、"_"、"-" 组成，且必须包含至少一个 "."。
     * 例如：{@code "common.success"}、{@code "validation.failed"}。
     * 自然语言文本（如 "操作成功"、"Order created"）不含 "." 或含空格，不会匹配。
     * </p>
     */
    private boolean isI18nKey(String msg) {
        if (StringUtils.isBlank(msg) || !msg.contains(".")) {
            return false;
        }
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '.' || c == '_' || c == '-'
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')) {
                continue;
            }
            // 含非 ASCII 字符或空格等 → 不是 i18n key
            return false;
        }
        return true;
    }
}
