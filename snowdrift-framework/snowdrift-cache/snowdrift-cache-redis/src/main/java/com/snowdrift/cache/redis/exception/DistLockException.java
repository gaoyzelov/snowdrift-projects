package com.snowdrift.cache.redis.exception;

import com.snowdrift.core.enums.ErrorCodeEnum;
import com.snowdrift.core.exception.BaseException;

/**
 * DiskLockException
 *
 * @author gaoye
 * @date 2025/03/20 09:50:10
 * @description 分布式锁异常
 * @since 1.0.0
 */
public class DistLockException extends BaseException {

    public DistLockException(String message) {
        super(message);
    }

    public DistLockException(Integer code, String message) {
        super(code, message);
    }

    public DistLockException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

    public DistLockException(String message, Throwable cause) {
        super(message, cause);
    }
}