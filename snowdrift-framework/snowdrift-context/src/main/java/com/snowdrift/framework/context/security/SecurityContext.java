package com.snowdrift.framework.context.security;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * 复制一份字段等价的上下文。
     * <p>
     * 用于「存储层把身份交付给调用方」的场景。本类是可变 {@code @Data}，若 {@code TokenStore}
     * 直接把存储内的实例交出去，业务代码对它调用任何 setter 都会<b>永久写回该 token 的会话</b>，
     * 污染同一会话的后续所有请求；并发请求之间还存在数据竞争。因此交付前先复制。
     * </p>
     * <p>
     * 可变容器（{@link #roleKeys}/{@link #permissions}/{@link #attributes}）会重建为新容器，
     * 其中的元素本身仍按引用共享 —— {@code attributes} 的值可能是任意业务对象，不做深拷贝。
     * </p>
     *
     * @return 字段等价、可变容器独立的新实例；对空上下文的复制同样返回空上下文
     */
    public SecurityContext copy() {
        return SecurityContext.builder()
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .tenantId(tenantId)
                .deptId(deptId)
                .roleKeys(roleKeys == null ? null : new ArrayList<>(roleKeys))
                .permissions(permissions == null ? null : new ArrayList<>(permissions))
                .platform(platform)
                .attributes(attributes == null ? null : new HashMap<>(attributes))
                .build();
    }

}
