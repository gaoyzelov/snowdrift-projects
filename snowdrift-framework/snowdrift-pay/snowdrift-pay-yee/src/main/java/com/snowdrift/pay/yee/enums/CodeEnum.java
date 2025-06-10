package com.snowdrift.pay.yee.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CodeEnum
 *
 * @author gaoye
 * @date 2025/06/06 09:21:25
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum CodeEnum {

    SUCCESS("000000", "成功"),
    OPR_SUCCESS("OPR00000",  "成功"),
    UA_SUCCESS("UA00000",  "成功"),
    SERVER_UNAVAILABLE("40020", "服务不可用"),
    PERMISSION_DENIED("40021", "授权权限不足"),
    ACCESS_LIMIT("40029", "访问受限"),
    ILLEGAL_ARGS("40042", "非法的参数"),
    HANDLE_FAILED("40044", "业务处理失败"),
    AUTH_FAILED("40047", "鉴权认证失败"),
    ENCRYPT_ERROR("40048", "加解密异常");

    private final String code;

    private final String note;
}