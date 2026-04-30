package com.snowdrift.framework.log.service;

import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.dto.OperateLogCreateDTO;
import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * LogRecordServiceImpl
 *
 * @author 83674
 * @date 2026/4/30-14:56
 * @description 操作日志记录服务实现类
 * @since 1.0.0
 */
public class LogRecordServiceImpl implements ILogRecordService {

    @Resource
    private ILogService logService;

    @Override
    public void record(LogRecord logRecord) {
        OperateLogCreateDTO operateLogDTO = OperateLogCreateDTO.builder()
                .traceId(LogTraceUtil.getTraceId())
                .bizId(Long.parseLong(logRecord.getBizNo()))
                .module(logRecord.getType())
                .action(logRecord.getSubType())
                .content(logRecord.getAction())
                .build();
        SecurityContext context = SecurityContextHolder.getContext();
        if (Objects.nonNull(context)){
            operateLogDTO.setUserId(context.getUserId());
            operateLogDTO.setUserType(context.getUserType());
            operateLogDTO.setTenantId(context.getTenantId());
            operateLogDTO.setOperator(context.getNickname());
            operateLogDTO.setOperateTime(LocalDateTime.now());
        }
        //记录日志
        logService.saveOperateLog(operateLogDTO);
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
