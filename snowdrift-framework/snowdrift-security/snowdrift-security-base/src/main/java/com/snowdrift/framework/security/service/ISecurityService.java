package com.snowdrift.framework.security.service;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.model.TokenInfo;

/**
 * 安全服务顶层抽象接口
 * <p>
 * 定义认证与鉴权的通用操作，屏蔽底层安全框架（Sa-Token / Spring Security）的差异。
 * 业务代码通过此接口获取当前用户上下文、校验登录状态、检查角色/权限，
 * 无需直接依赖具体框架的 API。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public interface ISecurityService {

    /**
     * 登录：生成 Token，将 SecurityContext 写入会话存储，返回 TokenInfo
     * <p>
     * 业务 Controller 在校验完用户名密码等凭证后，构造好 {@link SecurityContext}
     * （含用户信息、角色、权限），调用此方法完成登录。
     * </p>
     *
     * @param context 安全上下文（由业务层填充用户/角色/权限信息）
     * @return 统一的 Token 响应模型
     */
    TokenInfo login(SecurityContext context);

    /**
     * 当前请求是否已通过认证（已登录）
     *
     * @return true 已登录，false 未登录
     */
    boolean isAuthenticated();

    /**
     * 校验登录状态，未登录则抛出异常
     * <p>
     * 通常用于接口拦截器中，对需要登录的接口做前置校验。
     * </p>
     */
    void checkLogin();

    /**
     * 从当前会话中获取安全上下文
     * <p>
     * 包含用户ID、账号、昵称、组织/租户、角色、权限等 RBAC 数据，
     * 由登录时写入框架的会话存储中。
     * </p>
     *
     * @return 安全上下文，未登录时返回 null
     */
    SecurityContext getContext();

    /**
     * 检查当前用户是否拥有指定角色（编程式鉴权）
     *
     * @param role 角色标识
     * @return true 拥有该角色
     */
    boolean hasRole(String role);

    /**
     * 检查当前用户是否拥有指定权限（编程式鉴权）
     *
     * @param permission 权限标识
     * @return true 拥有该权限
     */
    boolean hasPermission(String permission);

    /**
     * 登出：使当前会话失效，清除登录状态
     */
    void logout();
}
