package com.snowdrift.framework.context.security;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SecurityContext
 *
 * @author gaoyzelov
 * @date 2026/4/30-15:55
 * @description 安全上下文
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 租户ID
     */
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    // ========== 授权信息（RBAC）==========
    /**
     * 角色Key列表
     */
    private List<String> roleKeys;
    /**
     * 权限标识列表
     */
    private List<String> permissions;

    // =================== 平台信息 ========================
    /**
     * 平台
     */
    private String platform;

    // =================== 扩展信息 ========================
    /**
     * 属性
     * <p>排除出 {@code toString()}：这里是业务自定义数据的集中存放点，可能包含令牌等机密
     * （如 spring 后端登录时写入的 {@code token}），上下文对象一旦被打印就会随日志外泄。</p>
     */
    @ToString.Exclude
    private Map<String, Object> attributes;

}
