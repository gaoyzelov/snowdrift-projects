package com.snowdrift.framework.security.exception;

import com.snowdrift.framework.common.exception.BizException;

/**
 * Security 异常
 *
 * @author gaoyzelov
 * @date 2026/5/15
 * @description 安全操作异常（支持国际化）
 *              message 字段存储国际化消息 key，而非硬编码文本
 * @since 1.0.0
 */
public class SecurityException extends BizException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Object[] args) {
        super(message, args);
    }
}
