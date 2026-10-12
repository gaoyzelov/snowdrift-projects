package com.snowdrift.framework.security.satoken.interceptor;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.annotation.Anonymous;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.service.ISecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SecurityInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Sa-Token 安全拦截器黑盒测试：@Anonymous 放行、登录校验、上下文写入/缺失处理、afterCompletion 清理
 * @since 1.0.0
 */
class SecurityInterceptorTest {

    static class DemoController {
        @Anonymous
        public void publicApi() {
        }

        public void privateApi() {
        }
    }

    private final ISecurityService securityService = mock(ISecurityService.class);
    private final SecurityInterceptor interceptor = new SecurityInterceptor(securityService);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    {
        // 关闭 Sa-Token 注解鉴权分支，聚焦本框架拦截器自身逻辑（注解策略需 Sa-Token 运行时）
        interceptor.isAnnotation = false;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new DemoController(), DemoController.class.getMethod(method));
    }

    @Test
    @DisplayName("@Anonymous 方法直接放行，不做登录校验、不写上下文")
    void anonymousPassesThrough() throws Exception {
        boolean result = interceptor.preHandle(request, response, handler("publicApi"));

        assertThat(result).isTrue();
        verify(securityService, never()).checkLogin();
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("已登录且能取到上下文：校验登录并写入 SecurityContextHolder")
    void authenticatedSetsContext() throws Exception {
        SecurityContext ctx = SecurityContext.builder().userId(1L).build();
        when(securityService.getContext()).thenReturn(ctx);

        boolean result = interceptor.preHandle(request, response, handler("privateApi"));

        assertThat(result).isTrue();
        verify(securityService).checkLogin();
        assertThat(SecurityContextHolder.getContext()).isSameAs(ctx);
    }

    @Test
    @DisplayName("已登录但上下文缺失：抛 SecurityException，不残留上下文")
    void missingContextThrows() throws Exception {
        when(securityService.getContext()).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("privateApi")))
                .isInstanceOf(SecurityException.class);
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("checkLogin 抛异常（未登录）：原样上抛，未写入上下文")
    void checkLoginFailurePropagates() throws Exception {
        doThrow(new SecurityException("未登录")).when(securityService).checkLogin();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("privateApi")))
                .isInstanceOf(SecurityException.class);
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("非 HandlerMethod（静态资源、RouterFunction 端点）同样校验登录：认证不因处理器类型而失效")
    void nonHandlerMethodStillRequiresLogin() throws Exception {
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        verify(securityService).checkLogin();
    }

    @Test
    @DisplayName("非 HandlerMethod 且未登录：checkLogin 抛出的异常原样上抛，不放行")
    void nonHandlerMethodLoginFailurePropagates() throws Exception {
        doThrow(new SecurityException("未登录")).when(securityService).checkLogin();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("afterCompletion 清理上下文，防止线程复用污染")
    void afterCompletionClears() throws Exception {
        SecurityContextHolder.setContext(SecurityContext.builder().userId(2L).build());

        interceptor.afterCompletion(request, response, handler("privateApi"), null);

        assertThat(SecurityContextHolder.peekContext()).isNull();
    }
}
