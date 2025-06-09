package com.snowdrift.oss.api.exception;

import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.exception.BaseException;

/**
 * OssException
 *
 * @author gaoye
 * @date 2025/03/25 15:15:12
 * @description 对象存储服务异常
 * @since 1.0.0
 */
public class OssException extends BaseException {

    public OssException(String message) {
        super(message);
    }

    public OssException(Integer code, String message) {
        super(code, message);
    }

    public OssException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
    }
}