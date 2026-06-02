package com.snowdrift.framework.context.security;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SecurityContext
 *
 * @author 83674
 * @date 2026/4/30-15:55
 * @description 安全上下文
 * @since 1.0.0
 */
@Data
@Builder
public class SecurityContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // =================== 基础信息 ========================
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    // =================== 组织信息 ========================
    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 租户ID
     */
    private Long tenantId;

    // ========== 授权信息（RBAC）==========
    /**
     * 角色Key列表
     */
    private List<String> roleKeys;
    /**
     * 权限标识列表
     */
    private List<String> permissions;

    // =================== 扩展信息 ========================
    /**
     * 属性
     */
    private Map<String, Object> attributes;

}
