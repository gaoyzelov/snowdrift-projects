package com.snowdrift.framework.security.spring.filter;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.properties.SecurityProperties;
import com.snowdrift.framework.security.spring.store.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SecurityContextFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 安全上下文桥接过滤器黑盒测试：放行路径、无/坏 token、有效 token 建立上下文、前缀解析、异常与 finally 清理
 * @since 1.0.0
 */
class SecurityContextFilterTest {

    private final SecurityProperties properties = new SecurityProperties();
    private final TokenStore tokenStore = mock(TokenStore.class);
    private final SecurityContextFilter filter = new SecurityContextFilter(properties, tokenStore);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** 捕获 chain.doFilter 执行期间（即下游可见）的框架上下文 */
    private AtomicReference<SecurityContext> captureDuringChain() throws Exception {
        AtomicReference<SecurityContext> captured = new AtomicReference<>();
        doAnswer(inv -> {
            captured.set(SecurityContextHolder.peekContext());
            return null;
        }).when(chain).doFilter(request, response);
        return captured;
    }

    @Test
    @DisplayName("放行路径（/error）：直接放行，不查询 TokenStore，不建立上下文")
    void excludedPathPassesThrough() throws Exception {
        when(request.getServletPath()).thenReturn("/error");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tokenStore, never()).get(any());
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("无 token 头：放行且不查询 TokenStore")
    void noTokenPassesThrough() throws Exception {
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tokenStore, never()).get(any());
    }

    @Test
    @DisplayName("坏 token（存储查不到）：放行但不建立上下文")
    void invalidTokenNoContext() throws Exception {
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(tokenStore.get("bad-token")).thenReturn(null);
        AtomicReference<SecurityContext> captured = captureDuringChain();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(captured.get()).isNull();
    }

    @Test
    @DisplayName("有效 token：下游可见框架上下文与 Spring 认证，请求结束后清理")
    void validTokenEstablishesContextThenClears() throws Exception {
        SecurityContext ctx = SecurityContext.builder().userId(9L).username("tom").build();
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(tokenStore.get("good-token")).thenReturn(ctx);
        AtomicReference<SecurityContext> captured = captureDuringChain();

        filter.doFilterInternal(request, response, chain);

        // 下游执行期间上下文已建立
        assertThat(captured.get()).isSameAs(ctx);
        assertThat(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication()).isNull(); // finally 已清理 Spring 上下文
        // 请求结束后框架上下文被清理，避免线程复用污染
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("token 无约定前缀时按整串（trim 后）作为 token 查询")
    void tokenWithoutPrefix() throws Exception {
        SecurityContext ctx = SecurityContext.builder().userId(10L).build();
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn("  raw-token  ");
        when(tokenStore.get("raw-token")).thenReturn(ctx);
        AtomicReference<SecurityContext> captured = captureDuringChain();

        filter.doFilterInternal(request, response, chain);

        verify(tokenStore).get("raw-token");
        assertThat(captured.get()).isSameAs(ctx);
    }

    @Test
    @DisplayName("token 前缀大小写不敏感：bearer/BEARER 均能正确去除前缀")
    void tokenPrefixCaseInsensitive() throws Exception {
        SecurityContext ctx = SecurityContext.builder().userId(12L).build();
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn("bearer good-token");
        when(tokenStore.get("good-token")).thenReturn(ctx);
        AtomicReference<SecurityContext> captured = captureDuringChain();

        filter.doFilterInternal(request, response, chain);

        verify(tokenStore).get("good-token");
        assertThat(captured.get()).isSameAs(ctx);
    }

    @Test
    @DisplayName("下游抛异常：异常上抛，finally 仍清理上下文")
    void exceptionPropagatesAndContextCleared() throws Exception {
        SecurityContext ctx = SecurityContext.builder().userId(11L).build();
        when(request.getServletPath()).thenReturn("/api/order");
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(tokenStore.get("t")).thenReturn(ctx);
        doThrow(new ServletException("downstream")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(ServletException.class);
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("通配排除模式（/v3/api-docs/**）命中子路径时放行")
    void excludePatternsParsedAtConstruction() throws Exception {
        when(request.getServletPath()).thenReturn("/v3/api-docs/swagger-config");

        filter.doFilterInternal(request, response, chain);

        // /v3/api-docs/** 命中默认排除列表 → 放行、不查询 token
        verify(tokenStore, never()).get(any());
        verify(chain).doFilter(request, response);
    }
}
