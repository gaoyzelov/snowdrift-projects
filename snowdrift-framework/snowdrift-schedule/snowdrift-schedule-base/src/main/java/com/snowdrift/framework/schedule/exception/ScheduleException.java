package com.snowdrift.framework.schedule.exception;

import com.snowdrift.framework.base.exception.BizException;

/**
 * ScheduleException
 *
 * @author gaoyzelov
 * @date 2026/8/7-13:57
 * @description 调度异常，message 为可直接展示的中文描述（不使用 i18n key）
 * @since 1.0.0
 */
public class ScheduleException extends BizException {

    public ScheduleException(String message) {
        super(message);
    }
}
