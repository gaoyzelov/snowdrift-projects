package com.snowdrift.framework.schedule.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScheduleExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 调度异常黑盒测试：继承 BizException、message 直传（非 i18n key）、默认错误码、
 * null 消息边界与跨进程序列化契约
 * @since 1.0.0
 */
class ScheduleExceptionTest {

    @Test
    @DisplayName("继承 BizException，携带原始中文消息与默认错误码")
    void inheritsBizExceptionWithMessageAndDefaultCode() {
        ScheduleException e = new ScheduleException("Quartz 任务已存在");

        assertThat(e).isInstanceOf(BizException.class);
        assertThat(e).isInstanceOf(RuntimeException.class);
        assertThat(e.getMessage()).isEqualTo("Quartz 任务已存在");
        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
    }

    @Test
    @DisplayName("可被 BizException / RuntimeException 捕获（统一异常契约）")
    void catchableAsBizException() {
        Throwable caught = null;
        try {
            throw new ScheduleException("boom");
        } catch (BizException be) {
            caught = be;
        }

        assertThat(caught).isInstanceOf(ScheduleException.class);
    }

    @Test
    @DisplayName("边界：null 消息允许通过，错误码仍为默认 ERR")
    void nullMessageBoundary() {
        ScheduleException e = new ScheduleException(null);

        assertThat(e.getMessage()).isNull();
        assertThat(e.getCode()).isEqualTo(ResultCode.ERR.code());
    }

    @Test
    @DisplayName("Serializable 契约：异常可跨进程序列化往返，消息与错误码保持一致")
    void serializationRoundTrip() {
        ScheduleException original = new ScheduleException("任务不存在");

        ScheduleException restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original);
        assertThat(restored.getMessage()).isEqualTo("任务不存在");
        assertThat(restored.getCode()).isEqualTo(ResultCode.ERR.code());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject(value);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Serializable 往返失败", e);
        }
    }
}
