package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.context.security.SecurityContext;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 工具类
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
public final class SpringSecurityHelper {

    private SpringSecurityHelper() {
    }

    /**
     * 将 SecurityContext 中的角色、权限写入 Spring Security 的认证上下文，
     * 确保 @PreAuthorize 注解能在当前请求中生效
     */
    public static void setSpringSecurityAuthentication(SecurityContext sc) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sc.getRoleKeys())) {
            sc.getRoleKeys().forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }
        if (CollectionUtils.isNotEmpty(sc.getPermissions())) {
            sc.getPermissions().forEach(perm ->
                    authorities.add(new SimpleGrantedAuthority(perm)));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(sc.getUsername(), null, authorities);
        authentication.setDetails(sc);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
