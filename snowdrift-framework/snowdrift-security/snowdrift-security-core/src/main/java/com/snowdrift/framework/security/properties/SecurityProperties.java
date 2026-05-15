package com.snowdrift.framework.security.properties;

import lombok.Data;

/**
 * Security 统一配置
 * <p>
 * 定义安全模块的通用配置项，屏蔽底层框架差异
 *
 * @author 83674
 * @date 2026/5/15
 * @description 安全配置属性
 * @since 1.0.0
 */
@Data
public class SecurityProperties {

    /**
     * 是否启用安全模块
     */
    private boolean enabled = true;


    /**
     * Token 名称（请求头名称）
     */
    private String headerName = "Authorization";

    /**
     * Token 前缀
     */
    private String prefix = "Bearer";

    /**
     * Token 超时时间（秒）
     * 默认 24 小时
     */
    private long timeout = 86400;

    /**
     * Token 活跃超时时间（秒）
     * 用户最后一次访问后，Token 保持活跃的时间
     * 默认 30 分钟
     */
    private long activeTimeout = 1800;

    /**
     * 是否允许多端登录
     * true: 同一用户可以在多个设备同时登录
     * false: 同一用户只能在一个设备登录，新登录会踢出旧会话
     */
    private boolean concurrent = true;

}
