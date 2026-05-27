package com.snowdrift.framework.security.spring.service;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.model.TokenInfo;
import com.snowdrift.framework.security.service.ISecurityService;
import com.snowdrift.framework.security.spring.properties.SpringSecurityProperties;
import com.snowdrift.framework.security.spring.filter.SecurityContextFilter;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.security.spring.util.SpringSecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Spring Security 实现的 {@link ISecurityService}
 * <p>
 * 认证数据从 Spring Security 的 {@link SecurityContextHolder} 获取，
 * 鉴权委托给 Spring Security 的 {@code GrantedAuthority} 模型。
 * 登录时生成 UUID Token，通过 {@link TokenStore} 持久化映射关系。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class SpringSecurityServiceImpl implements ISecurityService {

    private final SpringSecurityProperties securityProperties;
    private final TokenStore tokenStore;

    public SpringSecurityServiceImpl(SpringSecurityProperties securityProperties, TokenStore tokenStore) {
        this.securityProperties = securityProperties;
        this.tokenStore = tokenStore;
    }

    /**
     * 登录：生成 UUID Token，将 SecurityContext 存入 TokenStore，返回 TokenInfo
     * <p>
     * 业务 Controller 在校验完用户名密码等凭证后，构造好 {@link SecurityContext}
     * （含用户信息、角色、权限），调用此方法完成登录。
     * </p>
     *
     * @param context 安全上下文（由业务层填充用户/角色/权限信息）
     * @return 统一的 Token 响应模型
     */
    @Override
    public TokenInfo login(SecurityContext context) {
        if (context == null || context.getUserId() == null) {
            throw new SecurityException("security.context.null");
        }
        String tokenValue = UUID.randomUUID().toString().replace("-", "");

        tokenStore.put(tokenValue, context, securityProperties.getTimeout());
        SpringSecurityHelper.setSpringSecurityAuthentication(context);

        return TokenInfo.builder()
                .tokenValue(tokenValue)
                .tokenName(securityProperties.getHeaderName())
                .prefix(securityProperties.getPrefix())
                .expiresIn(securityProperties.getTimeout())
                .build();
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }

    @Override
    public void checkLogin() {
        if (!isAuthenticated()) {
            throw new SecurityException("security.not.authenticated");
        }
    }

    @Override
    public SecurityContext getContext() {
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        // SecurityContext 作为 details 存储在 Authentication 中（由 SecurityContextFilter 设置）
        Object details = auth.getDetails();
        if (details instanceof SecurityContext sc) {
            return sc;
        }
        return null;
    }

    @Override
    public void logout() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String token = (String) attrs.getRequest().getAttribute(SecurityContextFilter.TOKEN_ATTRIBUTE);
            if (token != null) {
                tokenStore.remove(token);
            }
        }
        SecurityContextHolder.clearContext();
        com.snowdrift.framework.context.security.SecurityContextHolder.clear();
    }

    @Override
    public boolean hasRole(String role) {
        String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleAuthority));
    }

    @Override
    public boolean hasPermission(String permission) {
        org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

}
