package com.snowdrift.web.bo;

import com.snowdrift.core.enums.AccessTypeEnum;
import com.snowdrift.core.enums.ResultEnum;
import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty(value = "接口类型，0-通用接口，1-登录，2-登出", example = "0")
    private AccessTypeEnum type = AccessTypeEnum.GENERAL;

    @ApiModelProperty(value = "模块", example = "系统服务", position = 1)
    private String module;

    @ApiModelProperty(value = "操作描述", example = "用户登录", position = 2)
    private String action;

    @ApiModelProperty(value = "请求方法", example = "com.snowdrift.web.controller.UserController.login", position = 3)
    private String method;

    @ApiModelProperty(value = "请求URI", example = "/login", position = 4)
    private String requestUri;

    @ApiModelProperty(value = "请求方式", example = "GET", position = 5)
    private String requestMethod;

    @ApiModelProperty(value = "请求参数", position = 6)
    private String requestParams;

    @ApiModelProperty(value = "请求IP", example = "127.0.0.1", position = 7)
    private String requestIp;

    @ApiModelProperty(value = "请求用户", example = "admin", position = 8)
    private String requestBy;

    @ApiModelProperty(value = "请求时间", example = "2024-08-19 15:35:00", position = 9)
    private LocalDateTime requestTime = LocalDateTime.now();

    @ApiModelProperty(value = "请求耗时，单位毫秒", example = "100", position = 10)
    private Long requestCost;

    @ApiModelProperty(value = "请求结果,0-失败，1-成功", example = "1", position = 11)
    private ResultEnum result = ResultEnum.OK;

    @ApiModelProperty(value = "异常信息", example = "java.lang.NullPointerException", position = 12)
    private String exception;
}