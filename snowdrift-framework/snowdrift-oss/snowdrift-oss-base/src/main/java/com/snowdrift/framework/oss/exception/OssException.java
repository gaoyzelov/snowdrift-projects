package com.snowdrift.framework.oss.exception;

import com.snowdrift.framework.base.exception.BizException;

/**
 * OSS 异常
 * <p>message 为可直接展示的中文描述（不使用 i18n key）。</p>
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @since 1.0.0
 */
public class OssException extends BizException {

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
    }
}
