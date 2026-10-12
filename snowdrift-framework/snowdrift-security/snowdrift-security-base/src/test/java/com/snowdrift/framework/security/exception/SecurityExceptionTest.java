package com.snowdrift.framework.security.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 安全异常黑盒测试：继承 BizException、message 与 ResultCode 两种构造的语义、
 * 统一异常捕获契约与 null 消息边界
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

    @Test
    @DisplayName("多种 ResultCode：错误码与消息分别取自对应常量")
    void variousResultCodesCarryTheirCode() {
        SecurityException forbidden = new SecurityException(ResultCode.FORBIDDEN);
        SecurityException notFound = new SecurityException(ResultCode.NOT_FOUND);
        SecurityException badRequest = new SecurityException(ResultCode.BAD_REQUEST);

        assertThat(forbidden.getCode()).isEqualTo(ResultCode.FORBIDDEN.code());
        assertThat(forbidden.getMessage()).isEqualTo(ResultCode.FORBIDDEN.msg());
        assertThat(notFound.getCode()).isEqualTo(ResultCode.NOT_FOUND.code());
        assertThat(notFound.getMessage()).isEqualTo(ResultCode.NOT_FOUND.msg());
        assertThat(badRequest.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
    }

    @Test
    @DisplayName("可被 BizException / RuntimeException 捕获（统一异常契约）")
    void catchableAsBizExceptionAndRuntimeException() {
        assertThatThrownBy(() -> {
            throw new SecurityException("boom");
        })
                .isInstanceOf(SecurityException.class)
                .isInstanceOf(BizException.class)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

    @Test
    @DisplayName("边界：null 消息允许通过，错误码仍为默认 ERR")
    void nullMessageBoundary() {
        SecurityException e = new SecurityException((String) null);

        assertThat(e.getMessage()).isNull();
        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
    }
}
