package com.snowdrift.framework.security.spring.service;

import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.model.TokenInfo;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.security.spring.util.SpringSecurityHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SpringSecurityServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Spring Security 实现黑盒测试：登录/登出、认证态（排除匿名）、上下文读取、角色 ROLE_ 前缀与权限精确匹配
 * @since 1.0.0
 */
class SpringSecurityServiceImplTest {

    private final TokenStore tokenStore = mock(TokenStore.class);
    private final SpringSecurityProperties properties = new SpringSecurityProperties();
    private final SpringSecurityServiceImpl service = new SpringSecurityServiceImpl(properties, tokenStore);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        com.snowdrift.framework.context.security.SecurityContextHolder.clear();
    }

    private static SecurityContext ctx(Long userId) {
        return SecurityContext.builder().userId(userId).username("tom").build();
    }

    // ========== login ==========

    @Test
    @DisplayName("login：上下文为空或缺 userId 时抛 UNAUTHORIZED")
    void loginRejectsInvalidContext() {
        assertThatThrownBy(() -> service.login(null))
                .isInstanceOf(SecurityException.class)
                .extracting(e -> ((SecurityException) e).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED.code());
        assertThatThrownBy(() -> service.login(ctx(null)))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("login：生成 32 位无连字符 token，写入 TokenStore 并回填 attributes/认证信息")
    void loginSuccess() {
        SecurityContext context = ctx(1L);

        TokenInfo info = service.login(context);

        assertThat(info.getTokenValue()).hasSize(32).doesNotContain("-");
        assertThat(info.getTokenName()).isEqualTo("Authorization");
        assertThat(info.getPrefix()).isEqualTo("Bearer");
        assertThat(info.getExpiresIn()).isEqualTo(properties.getTimeout());
        verify(tokenStore).put(eq(info.getTokenValue()), eq(context), eq(Duration.ofSeconds(properties.getTimeout())));
        assertThat(context.getAttributes()).containsEntry("token", info.getTokenValue());
        // 登录后 Spring 认证上下文已建立
        assertThat(service.isAuthenticated()).isTrue();
    }

    // ========== isAuthenticated / checkLogin ==========

    @Test
    @DisplayName("isAuthenticated：无认证信息返回 false")
    void notAuthenticatedWhenNoAuth() {
        assertThat(service.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated：匿名令牌不算已认证")
    void anonymousIsNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(service.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated：真实认证返回 true")
    void authenticatedWhenRealAuth() {
        SpringSecurityHelper.setSpringSecurityAuthentication(ctx(1L));

        assertThat(service.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("checkLogin：未登录抛 UNAUTHORIZED，已登录不抛")
    void checkLoginBehavior() {
        assertThatThrownBy(service::checkLogin).isInstanceOf(SecurityException.class);

        SpringSecurityHelper.setSpringSecurityAuthentication(ctx(1L));
        assertThatCode(service::checkLogin).doesNotThrowAnyException();
    }

    // ========== getContext ==========

    @Test
    @DisplayName("getContext：无认证返回 null；details 为 SecurityContext 时返回；否则 null")
    void getContextBehavior() {
        assertThat(service.getContext()).isNull();

        SecurityContext context = ctx(2L);
        SpringSecurityHelper.setSpringSecurityAuthentication(context);
        assertThat(service.getContext()).isSameAs(context);
    }

    // ========== logout ==========

    @Test
    @DisplayName("logout：从 attributes 取 token 移除存储，并清理两处上下文")
    void logoutRemovesTokenAndClears() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("token", "tok-123");
        SecurityContext context = SecurityContext.builder().userId(3L).username("tom").attributes(attrs).build();
        SpringSecurityHelper.setSpringSecurityAuthentication(context);
        com.snowdrift.framework.context.security.SecurityContextHolder.setContext(context);

        service.logout();

        verify(tokenStore).remove("tok-123");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(com.snowdrift.framework.context.security.SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("logout：无上下文时不移除 token，但仍清理，不抛异常")
    void logoutWithoutContext() {
        assertThatCode(service::logout).doesNotThrowAnyException();

        verify(tokenStore, never()).remove(org.mockito.ArgumentMatchers.anyString());
    }

    // ========== hasRole / hasPermission ==========

    @Test
    @DisplayName("hasRole：ROLE_ 前缀自动补全，命中返回 true，未命中/无认证返回 false")
    void hasRoleBehavior() {
        SecurityContext context = SecurityContext.builder().userId(4L).username("tom")
                .roleKeys(List.of("ADMIN")).build();
        SpringSecurityHelper.setSpringSecurityAuthentication(context);

        assertThat(service.hasRole("ADMIN")).isTrue();
        assertThat(service.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(service.hasRole("OTHER")).isFalse();

        SecurityContextHolder.clearContext();
        assertThat(service.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("hasPermission：精确匹配（不支持通配），无认证返回 false")
    void hasPermissionBehavior() {
        SecurityContext context = SecurityContext.builder().userId(5L).username("tom")
                .permissions(List.of("order:create")).build();
        SpringSecurityHelper.setSpringSecurityAuthentication(context);

        assertThat(service.hasPermission("order:create")).isTrue();
        assertThat(service.hasPermission("order:*")).isFalse();

        SecurityContextHolder.clearContext();
        assertThat(service.hasPermission("order:create")).isFalse();
    }

    @Test
    @DisplayName("健壮性：hasRole/hasPermission 入参为 null 或空白时返回 false，不抛 NPE")
    void nullSafeRoleAndPermission() {
        SecurityContext context = SecurityContext.builder().userId(6L).username("tom")
                .roleKeys(List.of("ADMIN")).permissions(List.of("order:create")).build();
        SpringSecurityHelper.setSpringSecurityAuthentication(context);

        assertThat(service.hasRole(null)).isFalse();
        assertThat(service.hasRole("   ")).isFalse();
        assertThat(service.hasPermission(null)).isFalse();
        assertThat(service.hasPermission("")).isFalse();
        // 正常值仍生效
        assertThat(service.hasRole("ADMIN")).isTrue();
        assertThat(service.hasPermission("order:create")).isTrue();
    }
}
