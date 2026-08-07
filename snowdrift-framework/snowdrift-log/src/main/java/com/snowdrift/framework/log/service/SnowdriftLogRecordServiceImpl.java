package com.snowdrift.framework.log.service;

import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.common.util.DateTimeUtil;
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
            SecurityContext context = SecurityContextHolder.getContext();
            OperateLogHolder holder = OperateLogHolder.builder()
                    .traceId(LogTraceUtil.getTraceId())
                    .bizId(logRecord.getBizNo())
                    .bizModule(logRecord.getType())
                    .bizType(logRecord.getSubType())
                    .content(logRecord.getAction())
                    .userId(context.getUserId())
                    .tenantId(context.getTenantId())
                    .operator(StringUtils.isNotBlank(context.getNickname()) ? context.getNickname() : context.getUsername())
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
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        throw new UnsupportedOperationException();
    }
}
