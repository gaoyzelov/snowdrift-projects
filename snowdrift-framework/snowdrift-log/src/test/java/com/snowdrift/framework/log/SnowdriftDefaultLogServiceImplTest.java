package com.snowdrift.framework.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * SnowdriftDefaultLogServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 默认日志服务单元测试（仅在 debug 级别输出）
 * @since 1.0.0
 */
class SnowdriftDefaultLogServiceImplTest {

    private final SnowdriftDefaultLogServiceImpl logService = new SnowdriftDefaultLogServiceImpl();

    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(SnowdriftDefaultLogServiceImpl.class);
        originalLevel = logger.getLevel();
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    @DisplayName("debug 开启时三类日志均以 JSON 落日志")
    void logsAtDebugLevel() {
        logger.setLevel(Level.DEBUG);

        logService.saveApiLog(ApiLogHolder.builder().uri("/api/order").status(1).build());
        logService.saveLoginLog(LoginLogHolder.builder().username("admin").status(1).build());
        logService.saveOperateLog(OperateLogHolder.builder().bizId("1001").content("修改订单").build());

        assertThat(appender.list).hasSize(3);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> assertThat(message).contains("接口日志：", "/api/order"))
                .anySatisfy(message -> assertThat(message).contains("登录日志：", "admin"))
                .anySatisfy(message -> assertThat(message).contains("操作日志：", "修改订单"));
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    }

    @Test
    @DisplayName("debug 关闭时不产生任何日志（默认实现不落库，仅在排障时输出）")
    void silentWhenDebugDisabled() {
        logger.setLevel(Level.INFO);

        logService.saveApiLog(ApiLogHolder.builder().uri("/api/order").build());
        logService.saveLoginLog(LoginLogHolder.builder().username("admin").build());
        logService.saveOperateLog(OperateLogHolder.builder().bizId("1001").build());

        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("未知字段（null）不抛异常")
    void toleratesEmptyHolders() {
        logger.setLevel(Level.DEBUG);

        assertThatCode(() -> {
            logService.saveApiLog(ApiLogHolder.builder().build());
            logService.saveLoginLog(LoginLogHolder.builder().build());
            logService.saveOperateLog(OperateLogHolder.builder().build());
        }).doesNotThrowAnyException();

        assertThat(appender.list).hasSize(3);
    }
}
