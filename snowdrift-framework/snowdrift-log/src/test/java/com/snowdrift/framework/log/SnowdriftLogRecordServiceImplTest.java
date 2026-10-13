package com.snowdrift.framework.log;

import com.mzt.logapi.beans.LogRecord;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import com.snowdrift.framework.log.util.LogTraceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowdriftLogRecordServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description bizlog 操作日志桥接单元测试
 * @since 1.0.0
 */
class SnowdriftLogRecordServiceImplTest {

    static final class CapturingLogService implements ILogService {
        final List<OperateLogHolder> logs = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void saveApiLog(ApiLogHolder holder) {
        }

        @Override
        public void saveLoginLog(LoginLogHolder holder) {
        }

        @Override
        public void saveOperateLog(OperateLogHolder holder) {
            if (failure != null) {
                throw failure;
            }
            logs.add(holder);
        }

        OperateLogHolder single() {
            assertThat(logs).hasSize(1);
            return logs.get(0);
        }
    }

    private CapturingLogService logService;
    private SnowdriftLogRecordServiceImpl service;
    private ch.qos.logback.classic.Logger serviceLogger;
    private ch.qos.logback.classic.Level originalLevel;

    @BeforeEach
    void setUp() {
        logService = new CapturingLogService();
        service = new SnowdriftLogRecordServiceImpl(logService);
        // “存储失败被吞掉”的用例会触发 error 日志，静音以保持构建输出干净
        serviceLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(SnowdriftLogRecordServiceImpl.class);
        originalLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clear();
        LogTraceUtil.clearTraceId();
        serviceLogger.setLevel(originalLevel);
    }

    private static LogRecord logRecord() {
        return LogRecord.builder()
                .id(1L)
                .bizNo("1001")
                .type("订单")
                .subType("UPDATE")
                .action("修改订单")
                .operator("bizlog-operator")
                .fail(false)
                .createTime(new Date(1_700_000_000_000L))
                .build();
    }

    @Test
    @DisplayName("字段映射：bizNo/type/subType/action 与时间正确落到 OperateLogHolder")
    void mapsFields() {
        SecurityContextHolder.setContext(SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员").tenantId(100L).build());
        LogTraceUtil.setTraceId("trace-1");

        service.record(logRecord());

        OperateLogHolder holder = logService.single();
        assertThat(holder.getBizId()).isEqualTo("1001");
        assertThat(holder.getBizModule()).isEqualTo("订单");
        assertThat(holder.getBizType()).isEqualTo("UPDATE");
        assertThat(holder.getContent()).isEqualTo("修改订单");
        assertThat(holder.getTraceId()).isEqualTo("trace-1");
        assertThat(holder.getUserId()).isEqualTo(1L);
        assertThat(holder.getTenantId()).isEqualTo(100L);
        assertThat(holder.getOperator()).isEqualTo("管理员");
        assertThat(holder.getOperateTime()).isEqualTo(
                LocalDateTime.ofInstant(new Date(1_700_000_000_000L).toInstant(), ZoneId.systemDefault()));
    }

    @Test
    @DisplayName("昵称为空时操作人回落到账号")
    void operatorFallsBackToUsername() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("admin").build());

        service.record(logRecord());

        assertThat(logService.single().getOperator()).isEqualTo("admin");
    }

    @Test
    @DisplayName("系统任务无登录上下文时仍记录，用户字段为 null")
    void recordsWithoutSecurityContext() {
        service.record(logRecord());

        OperateLogHolder holder = logService.single();
        assertThat(holder.getBizId()).isEqualTo("1001");
        assertThat(holder.getUserId()).isNull();
        assertThat(holder.getTenantId()).isNull();
        assertThat(holder.getOperator()).isNull();
    }

    @Test
    @DisplayName("日志存储抛异常时被吞掉，不影响调用方")
    void swallowsLogServiceFailure() {
        logService.failure = new IllegalStateException("存储不可用");

        assertThatCode(() -> service.record(logRecord())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("查询能力未实现，抛 BizException")
    void queryIsNotImplemented() {
        assertThatThrownBy(() -> service.queryLog("1001", "订单"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("操作日志查询能力未实现");
        assertThatThrownBy(() -> service.queryLogByBizNo("1001", "订单", "UPDATE"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("操作日志查询能力未实现");
    }
}
