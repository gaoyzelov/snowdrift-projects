package com.snowdrift.framework.log.config;

import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.log.service.ILogService;
import com.snowdrift.framework.log.service.LogRecordServiceImpl;
import com.snowdrift.framework.log.service.StdoutLogServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * SnowdriftLogConfiguration
 *
 * @author 83674
 * @date 2026/4/30-14:46
 * @description 日志配置
 * @since 1.0.0
 */
@Configuration
public class SnowdriftLogConfiguration {

    @Bean
    @ConditionalOnMissingBean(ILogService.class)
    public ILogService logService() {
        return new StdoutLogServiceImpl();
    }

    @Bean
    @Primary
    public ILogRecordService logRecordService() {
        return new LogRecordServiceImpl();
    }
}
