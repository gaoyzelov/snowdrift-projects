package com.snowdrift.framework.oss.aliyun.config;

import com.snowdrift.framework.oss.aliyun.service.AliyunOssServiceImpl;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 阿里云 OSS 存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 自动注册阿里云 OSS 存储实例到策略工厂
 * @since 1.0.0
 */
@AutoConfiguration
public class SnowdriftOssAliyunConfiguration {

    @Bean
    public OssServiceRegistration aliyunRegistration() {
        return new OssServiceRegistration(OssTypeEnum.ALIYUN, AliyunOssServiceImpl::new);
    }
}
