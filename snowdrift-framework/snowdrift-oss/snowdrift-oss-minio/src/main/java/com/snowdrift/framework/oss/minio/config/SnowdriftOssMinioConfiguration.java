package com.snowdrift.framework.oss.minio.config;

import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.minio.service.MinioOssServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * MinIO 存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 自动注册 MinIO 存储实例到策略工厂
 * @since 1.0.0
 */
@AutoConfiguration
public class SnowdriftOssMinioConfiguration {

    @Bean
    public OssServiceRegistration minioRegistration() {
        return new OssServiceRegistration(OssTypeEnum.MINIO, MinioOssServiceImpl::new);
    }
}
