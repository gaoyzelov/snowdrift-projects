package com.snowdrift.framework.security.satoken.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 框架异常全局处理
 * <p>
 * 拦截 Sa-Token 框架抛出的异常，统一转换为 {@link Result} 响应。
 * 与 {@code WebExceptionHandler} 配合使用，对 Sa-Token 的特定异常
 * 做更精确的映射（401 未登录、403 无权限/无角色等）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
@Order(1)
@Hidden
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 未登录异常（含 Token 过期、被踢、被顶等子类型）
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("[Sa-Token] 未登录: uri={}, type={}, message={}", request.getRequestURI(), e.getType(), e.getMessage());
        return Result.err(ResultCode.UNAUTHORIZED);
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("[Sa-Token] 权限不足: uri={}, permission={}", request.getRequestURI(), e.getPermission());
        return Result.err(ResultCode.FORBIDDEN);
    }

    /**
     * 角色不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("[Sa-Token] 角色不足: uri={}, role={}", request.getRequestURI(), e.getRole());
        return Result.err(ResultCode.FORBIDDEN);
    }

    /**
     * 其他 Sa-Token 异常（兜底）
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<Void> handleSaTokenException(SaTokenException e, HttpServletRequest request) {
        log.warn("[Sa-Token] 框架异常: uri={}, {}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
