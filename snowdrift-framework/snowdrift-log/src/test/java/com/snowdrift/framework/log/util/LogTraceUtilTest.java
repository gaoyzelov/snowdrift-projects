package com.snowdrift.framework.log.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * LogTraceUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 链路追踪工具类单元测试
 * @since 1.0.0
 */
class LogTraceUtilTest {

    @AfterEach
    void clearTraceId() {
        LogTraceUtil.clearTraceId();
    }

    @Test
    @DisplayName("工具类约定：final 且不可实例化")
    void notInstantiable() {
        assertThat(Modifier.isFinal(LogTraceUtil.class.getModifiers())).isTrue();
        assertThat(LogTraceUtil.class.getDeclaredConstructors())
                .isNotEmpty()
                .allSatisfy(ctor -> assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue());
    }

    @Test
    @DisplayName("setTraceId() 生成可解析的 UUID")
    void generatesUuid() {
        LogTraceUtil.setTraceId();

        String traceId = LogTraceUtil.getTraceId();
        assertThat(traceId).isNotBlank();
        assertThatCode(() -> UUID.fromString(traceId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("两次生成的 TraceId 不同")
    void generatesDistinctIds() {
        LogTraceUtil.setTraceId();
        String first = LogTraceUtil.getTraceId();
        LogTraceUtil.setTraceId();
        String second = LogTraceUtil.getTraceId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("setTraceId(自定义) 覆盖已有值并原样返回")
    void setCustomTraceId() {
        LogTraceUtil.setTraceId();
        LogTraceUtil.setTraceId("来自网关的链路ID");

        assertThat(LogTraceUtil.getTraceId()).isEqualTo("来自网关的链路ID");
    }

    @Test
    @DisplayName("clearTraceId 后返回 null，且可重复调用")
    void clears() {
        LogTraceUtil.setTraceId("t-1");

        LogTraceUtil.clearTraceId();
        assertThat(LogTraceUtil.getTraceId()).isNull();

        LogTraceUtil.clearTraceId();
        assertThat(LogTraceUtil.getTraceId()).isNull();
    }

    @Test
    @DisplayName("未设置时返回 null（不抛异常）")
    void unsetReturnsNull() {
        assertThat(LogTraceUtil.getTraceId()).isNull();
    }

    @Test
    @DisplayName("传入 null 不抛异常（MDC 视为清除）")
    void nullTraceIdIsTolerated() {
        LogTraceUtil.setTraceId("t-1");

        assertThatCode(() -> LogTraceUtil.setTraceId(null)).doesNotThrowAnyException();
        assertThat(LogTraceUtil.getTraceId()).isNull();
    }
}
