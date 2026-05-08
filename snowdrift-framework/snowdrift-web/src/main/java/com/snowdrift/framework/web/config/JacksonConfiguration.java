package com.snowdrift.framework.web.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.snowdrift.framework.common.util.DateTimeUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JacksonConfiguration
 *
 * @author 83674
 * @date 2026/5/7-18:00
 * @description Jackson 配置类
 * @since 1.0.0
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
public class JacksonConfiguration {

    /**
     * 自定义 Jackson ObjectMapper 配置
     *
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 注册模块
            builder.modules(getJavaTimeModule());
        };
    }


    /**
     * 获取 JavaTimeModule
     * 1. LocalDateTime → "yyyy-MM-dd HH:mm:ss"
     * 2. LocalDate → "yyyy-MM-dd"
     * 3. LocalTime → "HH:mm:ss"
     *
     * @return JavaTimeModule
     */
    private JavaTimeModule getJavaTimeModule() {
        // 创建 JavaTimeModule 用于注册自定义序列化器和反序列化器
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 注册 LocalDateTime 的序列化和反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeUtil.DATETIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeUtil.DATETIME_FORMATTER));

        // 注册 LocalDate 的序列化和反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeUtil.DATE_FORMATTER));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeUtil.DATE_FORMATTER));

        // 注册 LocalTime 的序列化和反序列化
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeUtil.TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeUtil.TIME_FORMATTER));
        return javaTimeModule;
    }
}
