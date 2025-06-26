package com.snowdrift.cache.redis.exception;

import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.exception.BaseException;

/**
 * LockException
 *
 * @author gaoye
 * @date 2025/06/26 14:08:38
 * @description xxxxxxxx
 * @since 1.0
 */
public class LockException extends BaseException {

    public LockException(String message) {
        super(message);
    }

    public LockException(Integer code, String message) {
        super(code, message);
    }

    public LockException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }
}