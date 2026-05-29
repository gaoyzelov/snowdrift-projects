package com.snowdrift.framework.security.satoken.service;

import cn.dev33.satoken.stp.StpUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.model.TokenInfo;
import com.snowdrift.framework.security.properties.SecurityProperties;
import com.snowdrift.framework.security.service.ISecurityService;
import lombok.extern.slf4j.Slf4j;

/**
 * Sa-Token 实现的 {@link ISecurityService}
 * <p>
 * 所有认证/鉴权操作均委托给 Sa-Token 的 {@link StpUtil} 工具类执行。
 * 登录/登出操作不在此接口中抽象，业务代码直接调用 {@code StpUtil.login()} 等原生 API。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class SaTokenSecurityServiceImpl implements ISecurityService {

    /**
     * SecurityContext 在 Sa-Token 会话中的存储键
     */
    private static final String CONTEXT_KEY = "context";

    private final SecurityProperties securityProperties;

    public SaTokenSecurityServiceImpl(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public TokenInfo login(SecurityContext context) {
        if (context == null || context.getUserId() == null) {
            throw new SecurityException("security.context.null");
        }
        // 1. 从 SecurityContext 中取 userId 作为 Sa-Token 的 loginId
        StpUtil.login(context.getUserId());

        // 2. 将 SecurityContext 存入当前会话，后续请求可通过 getContext() 取出
        StpUtil.getTokenSession().set(CONTEXT_KEY, context);

        // 3. 构造统一的 TokenInfo 返回给前端
        String tokenValue = StpUtil.getTokenValue();
        return TokenInfo.builder()
                .tokenValue(tokenValue)
                .tokenName(securityProperties.getHeaderName())
                .prefix(securityProperties.getPrefix())
                .expiresIn(securityProperties.getTimeout())
                .build();
    }

    @Override
    public boolean isAuthenticated() {
        return StpUtil.isLogin();
    }

    @Override
    public void checkLogin() {
        StpUtil.checkLogin();
    }

    @Override
    public SecurityContext getContext() {
        try {
            return StpUtil.getTokenSession().getModel(CONTEXT_KEY, SecurityContext.class, null);
        } catch (Exception e) {
            log.warn("获取安全上下文失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public boolean hasRole(String role) {
        // 委托 Sa-Token 的角色匹配逻辑（支持通配符等高级特性）
        return StpUtil.hasRole(role);
    }

    @Override
    public boolean hasPermission(String permission) {
        // 委托 Sa-Token 的权限匹配逻辑（支持通配符等高级特性）
        return StpUtil.hasPermission(permission);
    }
}
