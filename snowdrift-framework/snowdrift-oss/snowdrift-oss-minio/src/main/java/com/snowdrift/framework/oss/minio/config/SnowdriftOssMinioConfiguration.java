package com.snowdrift.framework.oss.minio.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.minio.service.MinioOssServiceImpl;
import com.snowdrift.framework.oss.properties.OssProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 存储自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 自动注册 MinIO 存储实例到策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SnowdriftOssMinioConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @PostConstruct
    public void registerMinioOssService() {
        ossStrategyFactory.registerFromProperties(ossProperties, OssTypeEnum.MINIO,
                MinioOssServiceImpl::new);
    }
}
