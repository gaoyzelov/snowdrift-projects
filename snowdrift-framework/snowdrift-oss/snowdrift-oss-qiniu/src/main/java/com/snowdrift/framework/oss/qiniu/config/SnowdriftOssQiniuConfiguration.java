package com.snowdrift.framework.oss.qiniu.config;

import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.qiniu.service.QiniuOssServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 七牛云 Kodo 存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 自动注册七牛云存储实例到策略工厂
 * @since 1.0.0
 */
@AutoConfiguration
public class SnowdriftOssQiniuConfiguration {

    @Bean
    public OssServiceRegistration qiniuRegistration() {
        return new OssServiceRegistration(OssTypeEnum.QINIU, QiniuOssServiceImpl::new);
    }
}
