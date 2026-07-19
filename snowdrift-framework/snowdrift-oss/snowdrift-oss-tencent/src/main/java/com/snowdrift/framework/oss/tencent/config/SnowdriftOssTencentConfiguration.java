package com.snowdrift.framework.oss.tencent.config;

import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.tencent.service.TencentOssServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 腾讯云 COS 存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/11
 * @description 自动注册腾讯云 COS 存储实例到策略工厂
 * @since 1.0.0
 */
@AutoConfiguration
public class SnowdriftOssTencentConfiguration {

    @Bean
    public OssServiceRegistration tencentRegistration() {
        return new OssServiceRegistration(OssTypeEnum.TENCENT, TencentOssServiceImpl::new);
    }
}
