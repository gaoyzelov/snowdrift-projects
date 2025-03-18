package com.snowdrift.core.result;

import com.snowdrift.core.constant.MsgConst;
import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.exception.BaseException;
import lombok.Builder;
import lombok.Data;

/**
 * JsonResult
 *
 * @author gaoye
 * @date 2025/03/18 16:28:59
 * @description 统一返回结果封装
 * @since 1.0.0
 */
@Data
@Builder
public class JsonResult<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 构建JsonResult
     *
     * @param <T> 数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> ok() {
        return ok(null);
    }

    /**
     * 构建JsonResult
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> ok(T data) {
        return ok(MsgConst.OK_MESSAGE, data);
    }

    /**
     * 构建JsonResult
     *
     * @param msg  消息
     * @param data 数据
     * @param <T>  数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> ok(String msg, T data) {
        return build(MsgConst.OK_CODE, msg, data);
    }

    /**
     * 构建JsonResult
     *
     * @param <T> 数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> err() {
        return err(null);
    }

    /**
     * 构建JsonResult
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> err(T data) {
        return err(MsgConst.ERR_MESSAGE, data);
    }

    /**
     * 构建JsonResult
     *
     * @param msg  消息
     * @param data 数据
     * @param <T>  数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> err(String msg, T data) {
        return build(MsgConst.ERR_CODE, msg, data);
    }

    /**
     * 构建JsonResult
     *
     * @param ex  异常
     * @param data 数据
     * @param <T> 数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> err(BaseException ex, T data) {
        return build(ex.getCode(), ex.getMessage(), data);
    }

    /**
     * 构建JsonResult
     *
     * @param err 错误码枚举
     * @param data 数据
     * @param <T> 数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> err(ErrorCodeEnum err, T data) {
        return build(err.getCode(), err.getNote(), data);
    }

    /**
     * 构建JsonResult
     *
     * @param code 状态码
     * @param msg  消息
     * @param data 数据
     * @param <T>  数据类型
     * @return JsonResult
     */
    public static <T> JsonResult<T> build(Integer code, String msg, T data) {
        return JsonResult.<T>builder()
                .code(code)
                .msg(msg)
                .data(data)
                .build();
    }
}