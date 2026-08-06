package com.snowdrift.framework.log.config;

import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.log.aspect.ApiLogAspect;
import com.snowdrift.framework.log.aspect.LoginLogAspect;
import com.snowdrift.framework.log.service.ILogService;
import com.snowdrift.framework.log.service.SnowdriftLogRecordServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * SnowdriftLogConfiguration
 *
 * @author gaoyzelov
 * @date 2026/4/30-14:46
 * @description 日志配置
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(ILogService.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SnowdriftLogConfiguration {

    @Bean
    @Primary
    public ILogRecordService logRecordService(ILogService logService) {
        return new SnowdriftLogRecordServiceImpl(logService);
    }

    @Bean
    public ApiLogAspect apiLogAspect(ILogService logService) {
        return new ApiLogAspect(logService);
    }

    @Bean
    public LoginLogAspect loginLogAspect(ILogService logService) {
        return new LoginLogAspect(logService);
    }
}
