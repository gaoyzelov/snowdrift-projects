package com.snowdrift.framework.mq.exception;

import com.snowdrift.framework.common.exception.BizException;

/**
 * 消息队列异常
 * <p>
 * message 字段存储 i18n key，配合 {@link com.snowdrift.framework.common.result.ResultCode} 使用。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
public class MqException extends BizException {

    public MqException(String message) {
        super(message);
    }

    public MqException(String message, Object[] args) {
        super(message, args);
    }

    public MqException(String message, Object[] args, Throwable cause) {
        super(message, args, cause);
    }
}
