package com.snowdrift.framework.log.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OperateLogCreateDTO
 *
 * @author 83674
 * @date 2026/4/30-14:52
 * @description 操作日志DTO
 * @since 1.0.0
 */
@Data
@Builder
@ToString
public class OperateLogCreateDTO implements Serializable {

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 操作对象ID
     */
    private Long bizId;

    /**
     * 操作模块
     */
    private String bizModule;

    /**
     * 操作类型
     */
    private String bizType;

    /**
     * 操作内容
     */
    private String content;

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
