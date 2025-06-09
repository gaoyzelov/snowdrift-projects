package com.snowdrift.web.bo;

import com.snowdrift.core.enums.AccessTypeEnum;
import com.snowdrift.core.enums.ResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * WebLog
 *
 * @author gaoye
 * @date 2025/03/25 09:26:38
 * @description xxxxxxxx
 * @since 1.0.0
 */
@Data
public class WebLog implements Serializable {

    @Schema(description = "接口类型，0-通用接口，1-登录，2-登出", example = "0")
    private AccessTypeEnum type = AccessTypeEnum.GENERAL;

    @Schema(description = "模块", example = "系统服务")
    private String module;

    @Schema(description = "操作描述", example = "用户登录")
    private String action;

    @Schema(description = "请求方法", example = "com.snowdrift.web.controller.UserController.login")
    private String method;

    @Schema(description = "请求URI", example = "/login")
    private String requestUri;

    @Schema(description = "请求方式", example = "GET")
    private String requestMethod;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "请求IP", example = "127.0.0.1")
    private String requestIp;

    @Schema(description = "请求用户", example = "admin")
    private String requestBy;

    @Schema(description = "请求时间", example = "2024-08-19 15:35:00")
    private LocalDateTime requestTime = LocalDateTime.now();

    @Schema(description = "请求耗时，单位毫秒", example = "100")
    private Long requestCost;

    @Schema(description = "请求结果,0-失败，1-成功", example = "1")
    private ResultEnum result = ResultEnum.OK;

    @Schema(description = "异常信息", example = "java.lang.NullPointerException")
    private String exception;
}