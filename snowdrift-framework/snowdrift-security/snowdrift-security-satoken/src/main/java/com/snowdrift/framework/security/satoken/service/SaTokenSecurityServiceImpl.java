package com.snowdrift.framework.security.satoken.service;

import cn.dev33.satoken.stp.StpUtil;
import com.snowdrift.framework.context.security.SecurityContext;
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

    /**
     * 构造注入公共安全配置，用于构造 {@link TokenInfo} 中的 tokenName、prefix 等字段
     */
    public SaTokenSecurityServiceImpl(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 登录：调用 Sa-Token 执行登录，将 SecurityContext 写入会话，返回统一 TokenInfo
     * <p>
     * 此方法不在 {@link ISecurityService} 中定义——登录机制因框架差异较大，
     * 由各实现模块按需暴露。业务 Controller 调用此方法即可完成登录并拿到 Token 响应。
     * </p>
     *
     * @param loginId 登录标识（通常为用户ID）
     * @param context 安全上下文（用户、角色、权限等 RBAC 数据）
     * @return 统一的 Token 响应模型
     */
    public TokenInfo login(Object loginId, SecurityContext context) {
        // 1. 执行 Sa-Token 登录，触发 Token 生成与会话创建
        StpUtil.login(loginId);

        // 2. 将 SecurityContext 存入当前会话，后续请求可通过 getContext() 取出
        StpUtil.getTokenSession().set(CONTEXT_KEY, context);

        // 3. 构造统一的 TokenInfo 返回给前端
        String tokenValue = StpUtil.getTokenValue();
        return TokenInfo.builder()
                .tokenValue(tokenValue)
                .tokenName(securityProperties.getHeaderName())
                .prefix(securityProperties.getPrefix())
                .expiresIn(StpUtil.getTokenTimeout())
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
            log.warn("获取安全上下文失败: {}", e.getMessage());
            return null;
        }
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
