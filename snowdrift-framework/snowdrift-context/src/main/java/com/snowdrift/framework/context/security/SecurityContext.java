package com.snowdrift.framework.context.security;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
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

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 属性
     */
    private Map<String, String> attributes;
}
