package com.snowdrift.framework.security.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一的 Token 响应模型
 * <p>
 * 登录成功后由各安全框架实现模块构造并返回给前端，
 * 屏蔽底层 Sa-Token、Spring Security 等不同实现中 Token 对象的差异。
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Data
@Builder
public class TokenInfo implements Serializable {

    /**
     * Token 值
     */
    private String tokenValue;

    /**
     * 请求头名称，如 "Authorization"
     */
    private String tokenName;

    /**
     * Token 前缀，如 "Bearer"
     */
    private String prefix;

    /**
     * 剩余有效时间（秒），-1 表示永不过期
     */
    private long expiresIn;
}
