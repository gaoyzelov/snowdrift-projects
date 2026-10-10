package com.snowdrift.framework.security.satoken.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SaTokenExceptionHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Sa-Token 异常处理黑盒测试：未登录→UNAUTHORIZED，权限/角色/封禁→FORBIDDEN，其它→INTERNAL_SERVER_ERROR
 * @since 1.0.0
 */
class SaTokenExceptionHandlerTest {

    private final SaTokenExceptionHandler handler = new SaTokenExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    @DisplayName("NotLoginException → UNAUTHORIZED")
    void notLoginMapsUnauthorized() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleNotLoginException(mock(NotLoginException.class), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.code());
    }

    @Test
    @DisplayName("NotPermissionException → FORBIDDEN")
    void notPermissionMapsForbidden() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleNotPermissionException(mock(NotPermissionException.class), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.code());
    }

    @Test
    @DisplayName("NotRoleException → FORBIDDEN")
    void notRoleMapsForbidden() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleNotRoleException(mock(NotRoleException.class), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.code());
    }

    @Test
    @DisplayName("DisableServiceException → FORBIDDEN + 封禁专用文案（不落入 500）")
    void disableMapsForbiddenWithMessage() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleDisableServiceException(mock(DisableServiceException.class), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.code());
        assertThat(result.getMsg()).isEqualTo("账号已被封禁，无法执行该操作");
    }

    @Test
    @DisplayName("其它 SaTokenException → INTERNAL_SERVER_ERROR 兜底")
    void otherSaTokenExceptionMapsServerError() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleSaTokenException(mock(SaTokenException.class), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.code());
    }
}
