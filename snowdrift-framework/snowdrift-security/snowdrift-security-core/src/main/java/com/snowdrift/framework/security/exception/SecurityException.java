package com.snowdrift.framework.security.exception;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.result.ResultCode;

/**
 * Security 异常
 *
 * @author 83674
 * @date 2026/5/15
 * @description 安全操作异常（支持国际化）
 *              message 字段存储国际化消息 key，而非硬编码文本
 * @since 1.0.0
 */
public class SecurityException extends BizException {

    public SecurityException() {
        super();
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Object[] args) {
        super(message, args);
    }

    public SecurityException(ResultCode resultCode) {
        super(resultCode);
    }

    public SecurityException(Integer code, String message) {
        super(code, message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }

    protected SecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
