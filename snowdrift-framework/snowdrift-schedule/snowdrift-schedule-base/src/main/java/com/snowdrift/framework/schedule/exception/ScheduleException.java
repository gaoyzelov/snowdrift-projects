package com.snowdrift.framework.schedule.exception;

import com.snowdrift.framework.common.exception.BizException;

/**
 * ScheduleException
 *
 * @author gaoyzelov
 * @date 2026/8/7-13:57
 * @description 调度异常（支持国际化）
 *              message 字段存储国际化消息 key，而非硬编码文本
 * @since 1.0.0
 */
public class ScheduleException extends BizException {

    public ScheduleException(String message) {
        super(message);
    }

    public ScheduleException(String message, Object[] args) {
        super(message, args);
    }

    public ScheduleException(String message, Object[] args, Throwable cause) {
        super(message, args, cause);
    }
}
