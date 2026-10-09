package com.snowdrift.framework.base.exception;

import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * BizExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 业务异常单元测试
 * @since 1.0.0
 */
class BizExceptionTest {

    @Test
    @DisplayName("无参构造使用 ERR 码与内置消息")
    void defaultConstructor() {
        BizException e = new BizException();

        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(e.getMessage()).isEqualTo(ResultCode.ERR.msg());
        assertThat(e.getArgs()).isNull();
    }

    @Test
    @DisplayName("仅消息构造使用 ERR 码")
    void messageConstructor() {
        BizException e = new BizException("订单不存在");

        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(e.getMessage()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("码+消息构造（下游兼容入口）")
    void codeAndMessageConstructor() {
        BizException e = new BizException(40001, "订单不存在");

        assertThat(e.getCode()).isEqualTo(40001);
        assertThat(e.getMessage()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("消息+参数构造保留 args 模板")
    void messageAndArgsConstructor() {
        Object[] args = {123L};
        BizException e = new BizException("订单 {0} 不存在", args);

        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(e.getMessage()).isEqualTo("订单 {0} 不存在");
        assertThat(e.getArgs()).containsExactly(123L);
    }

    @Test
    @DisplayName("ResultCode 构造沿用其码与消息，args 版本同时保留参数")
    void resultCodeConstructors() {
        BizException e = new BizException(ResultCode.LOCK_FAILED);
        assertThat(e.getCode()).isEqualTo(ResultCode.LOCK_FAILED.code());
        assertThat(e.getMessage()).isEqualTo(ResultCode.LOCK_FAILED.msg());

        BizException withArgs = new BizException(ResultCode.NOT_FOUND, new Object[]{"order"});
        assertThat(withArgs.getCode()).isEqualTo(ResultCode.NOT_FOUND.code());
        assertThat(withArgs.getArgs()).containsExactly("order");
    }

    @Test
    @DisplayName("cause 构造保留原因链")
    void causeConstructors() {
        IllegalStateException cause = new IllegalStateException("底层失败");

        BizException withMessage = new BizException("缓存序列化失败", cause);
        assertThat(withMessage.getMessage()).isEqualTo("缓存序列化失败");
        assertThat(withMessage.getCause()).isSameAs(cause);
        assertThat(withMessage.getCode()).isEqualTo(ResultCode.ERR.code());

        BizException withCauseOnly = new BizException(cause);
        assertThat(withCauseOnly.getMessage()).isEqualTo(ResultCode.ERR.msg());
        assertThat(withCauseOnly.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("throwEx 总是抛出，且可直接用于 return 位置")
    void throwExAlwaysThrows() {
        assertThatThrownBy(() -> BizException.throwEx("失败"))
                .isInstanceOf(BizException.class)
                .hasMessage("失败")
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ResultCode.ERR.code());

        assertThatThrownBy(() -> BizException.throwEx(ResultCode.FORBIDDEN))
                .isInstanceOf(BizException.class)
                .hasMessage(ResultCode.FORBIDDEN.msg());

        assertThatThrownBy(() -> BizException.throwEx(ResultCode.BAD_REQUEST, new Object[]{"id"}))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getArgs()).containsExactly("id"));

        assertThatThrownBy(() -> BizException.throwEx("模板 {0}", new Object[]{"x"}))
                .isInstanceOf(BizException.class)
                .hasMessage("模板 {0}");
    }

    @Test
    @DisplayName("throwEx 可作为泛型返回值直接 return")
    void throwExIsUsableAsReturnValue() {
        // 正常分支返回字符串
        assertThat(describe(false)).isEqualTo("ok");

        // 异常分支无需显式 new + throw：throwEx 的泛型返回值可直接放在 String 位置
        Throwable thrown = catchThrowable(() -> describe(true));
        assertThat(thrown).isInstanceOf(BizException.class).hasMessage("空串");
    }

    private static String describe(boolean blank) {
        return blank ? BizException.throwEx("空串") : "ok";
    }

    @Test
    @DisplayName("业务异常属于非受检异常")
    void isUnchecked() {
        assertThat(RuntimeException.class).isAssignableFrom(BizException.class);
    }

    @Test
    @DisplayName("可序列化：固定 serialVersionUID，Java 序列化往返后保留 code / message / args")
    void serializableRoundTrip() throws Exception {
        java.lang.reflect.Field uid = BizException.class.getDeclaredField("serialVersionUID");
        uid.setAccessible(true);
        assertThat(uid.getLong(null)).isEqualTo(1L);

        BizException source = new BizException(ResultCode.BAD_REQUEST, new Object[]{"id"});

        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(buffer)) {
            out.writeObject(source);
        }
        BizException restored;
        try (java.io.ObjectInputStream in =
                     new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(buffer.toByteArray()))) {
            restored = (BizException) in.readObject();
        }

        assertThat(restored.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(restored.getMessage()).isEqualTo(ResultCode.BAD_REQUEST.msg());
        assertThat(restored.getArgs()).containsExactly("id");
    }
}
