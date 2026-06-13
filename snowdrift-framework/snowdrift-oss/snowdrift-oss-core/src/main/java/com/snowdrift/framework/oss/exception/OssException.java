package com.snowdrift.framework.oss.exception;

import com.snowdrift.framework.common.exception.BizException;

/**
 * OSS 异常
 *
 * @author 83674
 * @date 2026/5/9
 * @description OSS 操作异常（支持国际化）
 *              message 字段存储国际化消息 key，而非硬编码文本
 * @since 1.0.0
 */
public class OssException extends BizException {

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Object[] args) {
        super(message, args);
    }

    public OssException(String message, Object[] args, Throwable cause) {
        super(message, args,cause);
    }

}
