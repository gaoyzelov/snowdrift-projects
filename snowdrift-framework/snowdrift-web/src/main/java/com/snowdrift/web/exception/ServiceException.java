package com.snowdrift.web.exception;

import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.exception.BaseException;

/**
 * ServiceException
 *
 * @author gaoye
 * @date 2025/03/24 14:21:28
 * @description 自定义服务异常
 * @since 1.0.0
 */
public class ServiceException extends BaseException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Integer code, String message) {
        super(code, message);
    }

    public ServiceException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}