package com.snowdrift.framework.log.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OperateLogDTO
 *
 * @author 83674
 * @date 2026/4/30-14:52
 * @description 操作日志DTO
 * @since 1.0.0
 */
@Data
@Builder
public class OperateLogCreateDTO implements Serializable {

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 操作对象ID
     */
    @NotNull(message = "操作对象ID不能为空")
    private Long bizId;

    /**
     * 操作模块
     */
    @NotBlank(message = "操作模块不能为空")
    private String module;

    /**
     * 操作类型
     */
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /**
     * 操作内容
     */
    @NotBlank(message = "操作内容不能为空")
    private String content;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 操作人员
     */
    @NotBlank(message = "操作人员不能为空")
    private String operator;

    /**
     * 操作时间
     */
    @NotNull(message = "操作时间不能为空")
    private LocalDateTime operateTime;
}
