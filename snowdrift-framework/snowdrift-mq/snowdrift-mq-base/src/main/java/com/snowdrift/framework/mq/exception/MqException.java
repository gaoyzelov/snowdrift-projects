package com.snowdrift.framework.mq.exception;

import com.snowdrift.framework.base.exception.BizException;

/**
 * 消息队列异常
 * <p>
 * message 使用可直接展示的中文描述（模块不依赖 i18n 国际化）。
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
}
