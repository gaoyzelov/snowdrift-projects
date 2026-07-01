package com.snowdrift.framework.schedule.xxljob.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * XXL-JOB 调度配置属性
 * <p>
 * 对照 XXL-JOB 官方 {@code application.properties} 配置项逐一定义，
 * 前缀 {@code snowdrift.schedule.xxl-job}，同时兼容 {@code xxl.job.*} 原生配置。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.schedule.xxl-job")
public class XxlJobProperties {

    /**
     * 是否启用 XXL-JOB
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * Admin 控制台地址（如 http://localhost:8080）
     * <p>多个地址用逗号分隔</p>
     */
    @NotBlank
    private String adminAddresses;

    /**
     * 执行器 AppName（需与 Admin 控制台配置一致）
     */
    @NotBlank
    private String appName = "snowdrift-job";

    /**
     * Admin 登录账号
     */
    @NotBlank
    private String username;

    /**
     * Admin 登录密码
     */
    @NotBlank
    private String password;

    /**
     * Admin 登录 Token Key
     * <p>登录成功后从响应 Cookie 中按此 key 提取 token 值</p>
     */
    private String loginTokenKey = "xxl_job_login_token";

    /**
     * Admin 登录 Token 超时时间
     * <p>登录成功后生成的 token 有效期</p>
     */
    private Duration loginTokenTimeout = Duration.ofHours(1);

    /**
     * Admin 通讯 Token（OpenApi 方式认证，后续兼容）
     */
    private String accessToken;

    /**
     * 执行器 IP（为空则自动获取）
     */
    private String ip;

    /**
     * 执行器端口（默认 9999，与内置 Tomcat 无关）
     */
    private Integer port = 9999;

    /**
     * 执行器手动注册地址（为空则使用 IP:PORT 自动注册）
     */
    private String address;

    /**
     * 任务日志存储路径
     */
    private String logPath = "logs/xxl-job";

    /**
     * 任务日志保留天数
     */
    private Integer logRetentionDays = 30;

    /**
     * Admin API 请求超时（秒）
     */
    private Integer adminTimeout = 5;
}