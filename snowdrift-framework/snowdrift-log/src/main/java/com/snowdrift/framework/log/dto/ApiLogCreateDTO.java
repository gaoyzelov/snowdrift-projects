package com.snowdrift.framework.log.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ApiLogCreateDTO
 *
 * @author 83674
 * @date 2026/4/30-14:52
 * @description 接口访问日志DTO
 * @since 1.0.0
 */
@Data
@Builder
@ToString
public class ApiLogCreateDTO implements Serializable {

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 应用名
     */
    private String appName;

    /**
     * 请求方法
     */
    private String method;
    /**
     * 访问地址
     */
    private String uri;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 响应结果
     */
    private String responseBody;
    /**
     * IP
     */
    private String ip;
    /**
     * 浏览器 UA
     */
    private String ua;

    /**
     * 操作模块
     */
    private String bizModule;

    /**
     * 操作类型
     */
    private Integer bizType;

    /**
     * 日志摘要
     */
    private String summary;

    /**
     * 执行时长，单位：毫秒
     */
    private Long duration;
    /**
     * 请求结果，0-失败，1-成功
     */
    private Integer status;
    /**
     * 异常信息
     */
    private String errorMsg;
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 操作人员
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
}
