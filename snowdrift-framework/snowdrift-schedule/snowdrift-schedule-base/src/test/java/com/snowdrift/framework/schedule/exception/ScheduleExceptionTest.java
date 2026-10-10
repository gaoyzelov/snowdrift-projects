package com.snowdrift.framework.schedule.exception;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScheduleExceptionTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 调度异常黑盒测试：继承 BizException、message 直传（非 i18n key）、默认错误码
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
}
