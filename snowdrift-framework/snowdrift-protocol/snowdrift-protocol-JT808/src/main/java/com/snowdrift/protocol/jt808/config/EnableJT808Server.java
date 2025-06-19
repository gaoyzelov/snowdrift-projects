package com.snowdrift.protocol.jt808.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * EnableJT808Server
 *
 * @author gaoye
 * @date 2025/06/16 15:57:34
 * @description xxxxxxxx
 * @since 1.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(JT808ServerAutoConfiguration.class)
public @interface EnableJT808Server {
}