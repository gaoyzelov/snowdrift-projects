package com.snowdrift.core.exception;

import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.enums.ResultEnum;
import lombok.Getter;

/**
 * BaseException
 *
 * @author gaoye
 * @date 2025/03/18 16:23:55
 * @description 自定义异常
 * @since 1.0.0
 */
@Getter
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    public BaseException(String message) {
        super(message);
        this.code = ResultEnum.ERR.getCode();
    }

    public BaseException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public BaseException(ErrorCodeEnum errorCode) {
        super(errorCode.getNote());
        this.code = errorCode.getCode();
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultEnum.ERR.getCode();
    }
}