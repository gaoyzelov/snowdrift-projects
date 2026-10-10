package com.snowdrift.framework.security.spring.util;

import com.snowdrift.framework.context.security.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringSecurityHelperTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description SecurityContext→Spring Authentication 映射黑盒测试：角色加 ROLE_ 前缀、权限原样、principal/details 与空集合
 * @since 1.0.0
 */
class SpringSecurityHelperTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("角色加 ROLE_ 前缀、权限原样写入 authorities，principal=用户名，details=上下文")
    void mapsRolesAndPermissions() {
        SecurityContext ctx = SecurityContext.builder()
                .username("tom")
                .roleKeys(List.of("ADMIN", "USER"))
                .permissions(List.of("order:create"))
                .build();

        SpringSecurityHelper.setSpringSecurityAuthentication(ctx);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo("tom");
        assertThat(auth.getDetails()).isSameAs(ctx);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "order:create");
    }

    @Test
    @DisplayName("边界：角色/权限均为空时 authorities 为空，但仍建立已认证的 Authentication")
    void emptyAuthoritiesStillAuthenticated() {
        SecurityContext ctx = SecurityContext.builder().username("anon").build();

        SpringSecurityHelper.setSpringSecurityAuthentication(ctx);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
