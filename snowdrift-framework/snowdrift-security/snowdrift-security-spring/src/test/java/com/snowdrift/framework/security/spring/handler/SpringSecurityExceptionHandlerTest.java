package com.snowdrift.framework.security.spring.handler;

import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpringSecurityExceptionHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Spring Security 异常处理黑盒测试：认证异常→UNAUTHORIZED，鉴权异常→FORBIDDEN
 * @since 1.0.0
 */
class SpringSecurityExceptionHandlerTest {

    private final SpringSecurityExceptionHandler handler = new SpringSecurityExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    @DisplayName("认证异常映射为 UNAUTHORIZED 业务码")
    void authenticationExceptionMapsUnauthorized() {
        when(request.getRequestURI()).thenReturn("/api/x");
        AuthenticationException ex = mock(AuthenticationException.class);

        Result<Void> result = handler.handleAuthenticationException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.code());
    }

    @Test
    @DisplayName("鉴权异常映射为 FORBIDDEN 业务码")
    void accessDeniedExceptionMapsForbidden() {
        when(request.getRequestURI()).thenReturn("/api/x");

        Result<Void> result = handler.handleAccessDeniedException(new AccessDeniedException("denied"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.FORBIDDEN.code());
    }
}
