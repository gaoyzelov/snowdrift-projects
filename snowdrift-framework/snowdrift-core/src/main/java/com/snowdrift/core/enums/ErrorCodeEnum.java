package com.snowdrift.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ErrorCodeEnum
 *
 * @author gaoye
 * @date 2025/03/18 16:19:47
 * @description 错误信息枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum implements IEnum<ErrorCodeEnum> {

    BAD_REQUEST(400, "错误请求"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "请求资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String note;
}