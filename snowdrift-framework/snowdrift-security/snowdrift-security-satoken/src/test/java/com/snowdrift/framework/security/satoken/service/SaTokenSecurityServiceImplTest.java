package com.snowdrift.framework.security.satoken.service;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.model.TokenInfo;
import com.snowdrift.framework.security.satoken.properties.SaTokenSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SaTokenSecurityServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Sa-Token 实现黑盒测试：登录参数校验与会话写入、上下文读取异常降级、鉴权委托
 * @since 1.0.0
 */
class SaTokenSecurityServiceImplTest {

    private final SaTokenSecurityProperties properties = new SaTokenSecurityProperties();
    private final SaTokenSecurityServiceImpl service = new SaTokenSecurityServiceImpl(properties);

    @Test
    @DisplayName("login：上下文为空或缺 userId 时抛 UNAUTHORIZED，不触发 Sa-Token 登录")
    void loginRejectsInvalidContext() {
        assertThatThrownBy(() -> service.login(null))
                .isInstanceOf(SecurityException.class)
                .extracting(e -> ((SecurityException) e).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED.code());
        assertThatThrownBy(() -> service.login(SecurityContext.builder().build()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("login：以 userId+platform 登录、写入会话上下文并构造 TokenInfo")
    void loginSuccess() {
        SecurityContext ctx = SecurityContext.builder().userId(1L).platform("PC").build();
        SaSession session = mock(SaSession.class);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            stp.when(StpUtil::getTokenValue).thenReturn("token-abc");

            TokenInfo info = service.login(ctx);

            stp.verify(() -> StpUtil.login(1L, "PC"));
            verify(session).set("context", ctx);
            assertThat(info.getTokenValue()).isEqualTo("token-abc");
            assertThat(info.getTokenName()).isEqualTo("Authorization");
            assertThat(info.getPrefix()).isEqualTo("Bearer");
            assertThat(info.getExpiresIn()).isEqualTo(properties.getTimeout());
        }
    }

    @Test
    @DisplayName("getContext：正常返回会话中的上下文")
    void getContextReturnsSessionModel() {
        SecurityContext ctx = SecurityContext.builder().userId(2L).build();
        SaSession session = mock(SaSession.class);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            when(session.getModel("context", SecurityContext.class, null)).thenReturn(ctx);

            assertThat(service.getContext()).isSameAs(ctx);
        }
    }

    @Test
    @DisplayName("getContext：底层抛 SaTokenException 时降级返回 null，不外泄异常")
    void getContextSwallowsSaTokenException() {
        SaSession session = mock(SaSession.class);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenSession).thenReturn(session);
            when(session.getModel("context", SecurityContext.class, null))
                    .thenThrow(new SaTokenException("session error"));

            assertThat(service.getContext()).isNull();
        }
    }

    @Test
    @DisplayName("isAuthenticated/checkLogin/logout 委托 StpUtil")
    void delegatesAuthOperations() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            assertThat(service.isAuthenticated()).isTrue();

            service.checkLogin();
            stp.verify(StpUtil::checkLogin);

            service.logout();
            stp.verify(StpUtil::logout);
        }
    }

    @Test
    @DisplayName("hasRole/hasPermission 委托 StpUtil 的匹配逻辑")
    void delegatesAuthorizationChecks() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(true);
            stp.when(() -> StpUtil.hasPermission("order:*")).thenReturn(true);

            assertThat(service.hasRole("ADMIN")).isTrue();
            assertThat(service.hasPermission("order:*")).isTrue();
        }
    }
}
