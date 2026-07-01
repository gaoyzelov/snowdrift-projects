package com.snowdrift.framework.security.spring.handler;

import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.common.result.ResultCode;
import com.snowdrift.framework.web.i18n.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 框架异常全局处理
 * <p>
 * 拦截 Spring Security 抛出的认证/鉴权异常，统一转换为 {@link Result} 响应，
 * 与 {@code WebExceptionHandler}、{@code SaTokenExceptionHandler} 协同工作。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
@Order(1)
@RestControllerAdvice
public class SpringSecurityExceptionHandler {

    /**
     * 认证异常（未登录 / Token 无效等）
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("[Spring Security] 认证失败: {}", e.getMessage());
        String message = I18nUtil.getMessage("security.not.authenticated");
        return Result.err(ResultCode.UNAUTHORIZED.code(), message);
    }

    /**
     * 鉴权异常（无权限 / 无角色）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[Spring Security] 权限不足: {}", e.getMessage());
        String message = I18nUtil.getMessage("security.permission.denied");
        return Result.err(ResultCode.FORBIDDEN.code(), message);
    }
}
