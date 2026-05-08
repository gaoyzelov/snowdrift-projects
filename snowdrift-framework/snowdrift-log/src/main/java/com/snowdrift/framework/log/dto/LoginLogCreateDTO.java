package com.snowdrift.framework.log.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LoginLogCreateDTO
 * @author 83674
 * @date 2026/4/30-14:51
 * @description 登录日志DTO
 * @since 1.0.0
 */
@Data
@Builder
@ToString
public class LoginLogCreateDTO implements Serializable {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户 IP
     */
    private String ip;

    /**
     * 用户 IP 地址
     */
    private String ipAddr;

    /**
     * 浏览器 UA
     */
    private String ua;

    /**
     * 登录结果,0-失败，1-成功
     */
    private Integer status;

    /**
     * 登录结果信息
     */
    private String msg;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
}
