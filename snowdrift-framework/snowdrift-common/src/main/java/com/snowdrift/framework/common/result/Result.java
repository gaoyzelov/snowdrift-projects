package com.snowdrift.framework.common.result;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Result
 *
 * @author 83674
 * @date 2026/3/25-16:09
 * @description 统一返回结果
 * @since 1.0.0
 */
@Data
@Builder
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 信息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    public static <T> Result<T> ok() {
        return create(ResultCode.OK.code(), ResultCode.OK.msg(), null);
    }

    public static <T> Result<T> ok(T data) {
        return create(ResultCode.OK.code(), ResultCode.OK.msg(), data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return create(ResultCode.OK.code(), msg, data);
    }

    public static <T> Result<T> err() {
        return create(ResultCode.ERR.code(), ResultCode.ERR.msg(), null);
    }

    public static <T> Result<T> err(Integer code, String msg) {
        return create(code, msg, null);
    }

    public static <T> Result<T> err(String msg) {
        return create(ResultCode.ERR.code(), msg, null);
    }

    public static <T> Result<T> err(String msg, T data) {
        return create(ResultCode.ERR.code(), msg, data);
    }

    public static <T> Result<T> err(ResultCode resultCode) {
        return create(resultCode.code(), resultCode.msg(), null);
    }

    public static <T> Result<T> err(ResultCode resultCode, T data) {
        return create(resultCode.code(), resultCode.msg(), data);
    }

    public static <T> Result<T> create(Integer code, String msg, T data) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
