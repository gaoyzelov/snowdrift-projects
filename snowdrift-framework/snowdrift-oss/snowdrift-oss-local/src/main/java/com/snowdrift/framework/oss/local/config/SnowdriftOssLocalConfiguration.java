package com.snowdrift.framework.oss.local.config;

import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.local.service.LocalOssServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 本地存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 自动注册本地存储实例到策略工厂
 * @since 1.0.0
 */
@AutoConfiguration
public class SnowdriftOssLocalConfiguration {

    @Bean
    public OssServiceRegistration localRegistration() {
        return new OssServiceRegistration(OssTypeEnum.LOCAL, LocalOssServiceImpl::new);
    }
}
