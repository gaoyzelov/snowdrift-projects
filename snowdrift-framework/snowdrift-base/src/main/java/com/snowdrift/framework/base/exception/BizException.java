package com.snowdrift.framework.base.exception;


import com.snowdrift.framework.base.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * BizException
 *
 * @author gaoyzelov
 * @date 2026/3/25-15:59
 * @description 业务异常
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误参数
     */
    private Object[] args;

    public BizException() {
        super(ResultCode.ERR.msg());
        this.code = ResultCode.ERR.code();
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERR.code();
    }

    /** 兼容下游模块直接指定业务码和消息的调用方式。 */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Object[] args) {
        super(message);
        this.code = ResultCode.ERR.code();
        this.args = args;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.msg());
        this.code = resultCode.code();
    }

    public BizException(ResultCode resultCode, Object[] args){
        super(resultCode.msg());
        this.code = resultCode.code();
        this.args = args;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.ERR.code();
    }

    public BizException(Throwable cause) {
        super(ResultCode.ERR.msg(), cause);
        this.code = ResultCode.ERR.code();
    }

    /**
     * 抛出业务异常
     *
     * @param message 错误信息
     */
    public static <T> T throwEx(String message) {
        throw new BizException(message);
    }

    /**
     * 抛出业务异常
     * @param message 错误信息
     * @param args 错误参数
     */
    public static  <T> T  throwEx(String message, Object[] args) {
        throw new BizException(message, args);
    }

    /**
     * 抛出业务异常
     * @param resultCode 错误码
     */
    public static <T> T throwEx(ResultCode resultCode) {
        throw new BizException(resultCode);
    }

    /**
     * 抛出业务异常
     * @param resultCode 错误码
     * @param args 错误参数
     */
    public static <T> T throwEx(ResultCode resultCode, Object[] args) {
        throw new BizException(resultCode, args);
    }
}
