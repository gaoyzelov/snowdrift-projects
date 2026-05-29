package com.snowdrift.framework.common.exception;


import com.snowdrift.framework.common.result.ResultCode;
import lombok.Getter;

/**
 * BizException
 *
 * @author 83674
 * @date 2026/3/25-15:59
 * @description 业务异常
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    private Object[] args;

    public BizException() {
        super();
        this.code = ResultCode.ERR.code();
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERR.code();
    }

    public BizException(String message, Object[] args) {
        super(message);
        this.code = ResultCode.ERR.code();
        this.args = args;
    }

    public BizException(ResultCode resultCode){
        super(resultCode.msg());
        this.code = resultCode.code();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.ERR.code();
    }

    public BizException(Throwable cause) {
        super(cause);
        this.code = ResultCode.ERR.code();
    }

    protected BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = ResultCode.ERR.code();
    }

    @Override
    public String getMessage() {
        if (code != null) {
            return "[" + code + "] " + super.getMessage();
        }
        return super.getMessage();
    }

    public String getRawMessage() {
        return super.getMessage();
    }
}
