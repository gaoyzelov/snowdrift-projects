package com.snowdrift.common.exception;


import com.snowdrift.common.result.ResultCode;
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

    public BizException() {
        super();
        this.code = ResultCode.BAD_REQUEST.code();
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.code();
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
        this.code = ResultCode.BAD_REQUEST.code();
    }

    public BizException(Throwable cause) {
        super(cause);
        this.code = ResultCode.BAD_REQUEST.code();
    }

    protected BizException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = ResultCode.BAD_REQUEST.code();
    }

    @Override
    public String getMessage() {
        if (code != null) {
            return "[" + code + "] " + super.getMessage();
        }
        return super.getMessage();
    }
}
