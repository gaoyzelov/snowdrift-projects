package com.snowdrift.framework.log.holder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogHolderTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 日志 DTO 单元测试（三个 Holder 的构建、序列化与字段完整性）
 * @since 1.0.0
 */
class LogHolderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 9, 12, 0);

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    @DisplayName("ApiLogHolder：builder 填充全部字段且可 Java 序列化往返")
    void apiLogHolder() throws Exception {
        ApiLogHolder holder = ApiLogHolder.builder()
                .traceId("t-1").appName("app").method("POST").uri("/order")
                .requestParams("{\"a\":1}").responseBody("{\"code\":1}")
                .ip("1.2.3.4").ua("ua").bizModule("订单").bizType(1).summary("创建")
                .duration(12L).status(1).errorMsg(null).userId(1L).tenantId(100L)
                .operator("管理员").operateTime(NOW)
                .build();

        assertThat(holder.getTraceId()).isEqualTo("t-1");
        assertThat(holder.getDuration()).isEqualTo(12L);
        assertThat(holder.getOperateTime()).isEqualTo(NOW);
        assertThat(holder.getErrorMsg()).isNull();

        ApiLogHolder copy = roundTrip(holder);
        assertThat(copy).isEqualTo(holder).isNotSameAs(holder);
        assertThat(copy.getOperateTime()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("LoginLogHolder：builder 填充全部字段且可 Java 序列化往返")
    void loginLogHolder() throws Exception {
        LoginLogHolder holder = LoginLogHolder.builder()
                .username("admin").ip("1.2.3.4").ipLocation("中国 北京").ua("ua")
                .status(1).msg("登录成功").loginTime(NOW)
                .build();

        assertThat(holder.getUsername()).isEqualTo("admin");
        assertThat(holder.getIpLocation()).isEqualTo("中国 北京");

        LoginLogHolder copy = roundTrip(holder);
        assertThat(copy).isEqualTo(holder).isNotSameAs(holder);
    }

    @Test
    @DisplayName("OperateLogHolder：builder 填充全部字段且可 Java 序列化往返")
    void operateLogHolder() throws Exception {
        OperateLogHolder holder = OperateLogHolder.builder()
                .traceId("t-1").bizId("1001").bizModule("订单").bizType("UPDATE")
                .content("修改订单").userId(1L).tenantId(100L).operator("管理员")
                .operateTime(NOW)
                .build();

        assertThat(holder.getBizId()).isEqualTo("1001");
        assertThat(holder.getBizType()).isEqualTo("UPDATE");

        OperateLogHolder copy = roundTrip(holder);
        assertThat(copy).isEqualTo(holder).isNotSameAs(holder);
    }

    @Test
    @DisplayName("三个 Holder 均为可序列化 DTO，并显式声明 serialVersionUID=1L 固定序列化兼容性")
    void serialVersionUidDeclared() throws Exception {
        for (Class<?> type : new Class<?>[]{ApiLogHolder.class, LoginLogHolder.class, OperateLogHolder.class}) {
            assertThat(Serializable.class).isAssignableFrom(type);

            var field = type.getDeclaredField("serialVersionUID");
            field.setAccessible(true);
            assertThat(field.getLong(null)).as("%s.serialVersionUID", type.getSimpleName()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("空 Holder 的字段为 null，toString 不抛异常")
    void emptyHolders() {
        assertThat(ApiLogHolder.builder().build().getStatus()).isNull();
        assertThat(LoginLogHolder.builder().build().getMsg()).isNull();
        assertThat(OperateLogHolder.builder().build().getContent()).isNull();

        assertThat(ApiLogHolder.builder().build().toString()).isNotNull();
        assertThat(LoginLogHolder.builder().build().toString()).isNotNull();
        assertThat(OperateLogHolder.builder().build().toString()).isNotNull();
    }
}
