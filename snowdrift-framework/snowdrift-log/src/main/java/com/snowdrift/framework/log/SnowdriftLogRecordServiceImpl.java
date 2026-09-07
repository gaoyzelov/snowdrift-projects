package com.snowdrift.framework.log;

import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.base.util.DateTimeUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import com.snowdrift.framework.log.util.LogTraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * SnowdriftLogRecordServiceImpl
 *
 * @author gaoyzelov
 * @date 2026/4/30-14:56
 * @description 操作日志记录服务实现类，需启用{@link com.mzt.logapi.starter.annotation.EnableLogRecord}
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftLogRecordServiceImpl implements ILogRecordService {

    private final ILogService logService;

    public SnowdriftLogRecordServiceImpl(ILogService logService) {
        this.logService = logService;
    }

    @Override
    public void record(LogRecord logRecord) {
        try {
            // 系统任务等无登录上下文的场景允许为空，操作日志仍记录、用户字段置 null
            SecurityContext context = SecurityContextHolder.peekContext();
            String operator = context != null && StringUtils.isNotBlank(context.getNickname())
                    ? context.getNickname()
                    : (context != null ? context.getUsername() : null);
            OperateLogHolder holder = OperateLogHolder.builder()
                    .traceId(LogTraceUtil.getTraceId())
                    .bizId(logRecord.getBizNo())
                    .bizModule(logRecord.getType())
                    .bizType(logRecord.getSubType())
                    .content(logRecord.getAction())
                    .userId(context != null ? context.getUserId() : null)
                    .tenantId(context != null ? context.getTenantId() : null)
                    .operator(operator)
                    .operateTime(DateTimeUtil.dateToLocalDateTime(logRecord.getCreateTime()))
                    .build();
            //记录日志
            logService.saveOperateLog(holder);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public List<LogRecord> queryLog(String bizNo, String type) {
        throw new UnsupportedOperationException("操作日志查询能力未实现，请注册自定义 ILogRecordService 实现");
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        throw new UnsupportedOperationException("操作日志查询能力未实现，请注册自定义 ILogRecordService 实现");
    }
}
