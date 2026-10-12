package com.snowdrift.framework.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftJacksonTimeConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Jackson 时间序列化配置黑盒测试：customizer 应用后 ObjectMapper 对 LocalDateTime/LocalDate/LocalTime
 * 按约定格式序列化与反序列化（往返对称）
 * @since 1.0.0
 */
class SnowdriftJacksonTimeConfigurationTest {

    private ObjectMapper buildMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new SnowdriftJacksonTimeConfiguration().customizer().customize(builder);
        return builder.build();
    }

    @Test
    @DisplayName("LocalDateTime 序列化为 yyyy-MM-dd HH:mm:ss 并可反序列化还原")
    void localDateTimeRoundTrip() throws Exception {
        ObjectMapper mapper = buildMapper();
        LocalDateTime value = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

        String json = mapper.writeValueAsString(value);
        assertThat(json).isEqualTo("\"2026-01-02 03:04:05\"");
        assertThat(mapper.readValue(json, LocalDateTime.class)).isEqualTo(value);
    }

    @Test
    @DisplayName("LocalDate 序列化为 yyyy-MM-dd 并可反序列化还原")
    void localDateRoundTrip() throws Exception {
        ObjectMapper mapper = buildMapper();
        LocalDate value = LocalDate.of(2026, 1, 2);

        String json = mapper.writeValueAsString(value);
        assertThat(json).isEqualTo("\"2026-01-02\"");
        assertThat(mapper.readValue(json, LocalDate.class)).isEqualTo(value);
    }

    @Test
    @DisplayName("LocalTime 序列化为 HH:mm:ss 并可反序列化还原")
    void localTimeRoundTrip() throws Exception {
        ObjectMapper mapper = buildMapper();
        LocalTime value = LocalTime.of(3, 4, 5);

        String json = mapper.writeValueAsString(value);
        assertThat(json).isEqualTo("\"03:04:05\"");
        assertThat(mapper.readValue(json, LocalTime.class)).isEqualTo(value);
    }

    @Test
    @DisplayName("POJO 中的时间字段同样按约定格式序列化")
    void pojoFieldFormatted() throws Exception {
        ObjectMapper mapper = buildMapper();

        String json = mapper.writeValueAsString(new Dto(LocalDateTime.of(2026, 12, 31, 23, 59, 58)));

        assertThat(json).contains("\"2026-12-31 23:59:58\"");
    }

    record Dto(LocalDateTime time) {
    }

    static class Money {
        private final String amount;

        Money(String amount) {
            this.amount = amount;
        }

        public String getAmount() {
            return amount;
        }
    }

    @Test
    @DisplayName("不覆盖宿主应用已注册的 Jackson 模块（Boot 用追加重载注册 @JsonComponent 与自定义 Module Bean）")
    void keepsHostApplicationModules() throws Exception {
        SimpleModule hostModule = new SimpleModule("host-module");
        hostModule.addSerializer(Money.class, new JsonSerializer<>() {
            @Override
            public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString("CNY:" + value.getAmount());
            }
        });

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        // 复现 Boot StandardJackson2ObjectMapperBuilderCustomizer 的注册方式（追加重载），它先于本框架的 customizer 执行
        builder.modulesToInstall(modules -> modules.add(hostModule));
        new SnowdriftJacksonTimeConfiguration().customizer().customize(builder);

        ObjectMapper mapper = builder.build();

        assertThat(mapper.writeValueAsString(new Money("1.00"))).isEqualTo("\"CNY:1.00\"");
        assertThat(mapper.writeValueAsString(LocalDateTime.of(2026, 1, 2, 3, 4, 5)))
                .isEqualTo("\"2026-01-02 03:04:05\"");
    }
}
