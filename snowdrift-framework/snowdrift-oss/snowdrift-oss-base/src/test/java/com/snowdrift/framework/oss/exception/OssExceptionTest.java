package com.snowdrift.framework.oss.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OssExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 异常黑盒测试：继承 BizException、两种构造的消息/cause 语义与默认错误码
 * @since 1.0.0
 */
class OssExceptionTest {

    @Test
    @DisplayName("message 构造：继承 BizException，携带消息与默认错误码")
    void messageConstructor() {
        OssException e = new OssException("上传失败");

        assertThat(e).isInstanceOf(BizException.class);
        assertThat(e.getMessage()).isEqualTo("上传失败");
        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
    }

    @Test
    @DisplayName("message+cause 构造：保留原始异常链")
    void messageAndCauseConstructor() {
        Throwable cause = new IllegalStateException("root");
        OssException e = new OssException("包装", cause);

        assertThat(e.getMessage()).isEqualTo("包装");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
