package com.snowdrift.framework.security.satoken.listener;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * TokenStateListenerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Token 状态监听器黑盒测试：登出/被踢/被顶清理上下文，被踢被顶删除 token 映射，其余事件安全无副作用
 * @since 1.0.0
 */
class TokenStateListenerTest {

    private final TokenStateListener listener = new TokenStateListener();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("doLogout：清理线程安全上下文")
    void logoutClearsContext() {
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());

        listener.doLogout("login", 1L, "token-value");

        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("doKickout：删除 token→id 映射并清理上下文")
    void kickoutDeletesMappingAndClears() {
        StpLogic stpLogic = mock(StpLogic.class);
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getStpLogic).thenReturn(stpLogic);

            listener.doKickout("login", 1L, "tok");

            verify(stpLogic).deleteTokenToIdMapping("tok");
        }
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("doReplaced：删除 token→id 映射并清理上下文")
    void replacedDeletesMappingAndClears() {
        StpLogic stpLogic = mock(StpLogic.class);
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getStpLogic).thenReturn(stpLogic);

            listener.doReplaced("login", 1L, "tok");

            verify(stpLogic).deleteTokenToIdMapping("tok");
        }
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("登录/封禁/解封/续期等事件仅记录日志，安全无副作用")
    void loggingEventsAreSafe() {
        assertThatCode(() -> {
            listener.doLogin("login", 1L, "tok", null);
            listener.doDisable("login", 1L, "service", 1, 100L);
            listener.doUntieDisable("login", 1L, "service");
            listener.doRenewTimeout("login", 1L, "tok", 100L);
            listener.doOpenSafe("login", "tok", "service", 100L);
            listener.doCloseSafe("login", "tok", "service");
            listener.doCreateSession("id");
            listener.doLogoutSession("id");
        }).doesNotThrowAnyException();
    }
}
