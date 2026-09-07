package com.snowdrift.framework.log.config;

import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.SnowdriftDefaultLogServiceImpl;
import com.snowdrift.framework.log.SnowdriftLogRecordServiceImpl;
import com.snowdrift.framework.log.aspect.ApiLogAspect;
import com.snowdrift.framework.log.aspect.LoginLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * SnowdriftLogConfiguration
 *
 * @author gaoyzelov
 * @date 2026/4/30-14:46
 * @description 日志配置
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SnowdriftLogConfiguration {

    /**
     * 默认日志服务
     * @return 日志服务
     */
    @Bean
    @ConditionalOnMissingBean(ILogService.class)
    public ILogService defaultLogService(){
        return new SnowdriftDefaultLogServiceImpl();
    }

    /**
     * 操作日志记录服务
     * @param logService 日志服务
     * @return 日志记录服务
     */
    @Bean
    @ConditionalOnMissingBean(ILogRecordService.class)
    public ILogRecordService logRecordService(ILogService logService) {
        return new SnowdriftLogRecordServiceImpl(logService);
    }

    /**
     * api日志切面
     * @param logService 日志服务
     * @return api日志切面
     */
    @Bean
    public ApiLogAspect apiLogAspect(ILogService logService) {
        return new ApiLogAspect(logService);
    }
    /**
     * 登录日志切面
     * @param logService 日志服务
     * @return 登录日志切面
     */
    @Bean
    public LoginLogAspect loginLogAspect(ILogService logService) {
        return new LoginLogAspect(logService);
    }



}
