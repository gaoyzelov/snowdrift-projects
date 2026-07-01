package com.snowdrift.framework.oss.aliyun.config;

import com.snowdrift.framework.oss.aliyun.service.AliyunOssServiceImpl;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 自动配置类
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 自动注册阿里云 OSS 实例到策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SnowdriftOssAliyunConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @PostConstruct
    public void registerAliyunOssService() {
        ossStrategyFactory.registerFromProperties(ossProperties, OssTypeEnum.ALIYUN,
                AliyunOssServiceImpl::new);
    }
}
