package com.snowdrift.framework.security.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;

/**
 * Security 异常
 *
 * @author gaoyzelov
 * @date 2026/5/15
 * @description 安全操作异常
 * @since 1.0.0
 */
public class SecurityException extends BizException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(ResultCode resultCode) {
        super(resultCode);
    }
}
