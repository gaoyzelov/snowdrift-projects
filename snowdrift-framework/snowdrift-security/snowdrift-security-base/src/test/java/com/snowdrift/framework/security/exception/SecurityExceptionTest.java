package com.snowdrift.framework.security.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 安全异常黑盒测试：继承 BizException、message 与 ResultCode 两种构造的语义
 * @since 1.0.0
 */
class SecurityExceptionTest {

    @Test
    @DisplayName("message 构造：继承 BizException，携带原始消息与默认错误码")
    void messageConstructor() {
        SecurityException e = new SecurityException("boom");

        assertThat(e).isInstanceOf(BizException.class);
        assertThat(e.getMessage()).isEqualTo("boom");
        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
    }

    @Test
    @DisplayName("ResultCode 构造：错误码与消息取自 ResultCode")
    void resultCodeConstructor() {
        SecurityException e = new SecurityException(ResultCode.UNAUTHORIZED);

        assertThat(e.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.code());
        assertThat(e.getMessage()).isEqualTo(ResultCode.UNAUTHORIZED.msg());
    }
}
