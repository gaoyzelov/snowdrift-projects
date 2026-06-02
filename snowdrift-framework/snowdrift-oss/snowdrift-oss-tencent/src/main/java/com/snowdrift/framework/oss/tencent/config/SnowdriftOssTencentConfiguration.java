package com.snowdrift.framework.oss.tencent.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.tencent.service.TencentOssServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 自动配置类
 *
 * @author 83674
 * @date 2026/5/11
 * @description 自动注册腾讯云 COS OSS 实例到策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(TencentOssServiceImpl.class)
public class SnowdriftOssTencentConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @PostConstruct
    public void registerTencentOssService() {
        ossStrategyFactory.registerFromProperties(ossProperties, OssTypeEnum.TENCENT,
                TencentOssServiceImpl::new);
    }
}
