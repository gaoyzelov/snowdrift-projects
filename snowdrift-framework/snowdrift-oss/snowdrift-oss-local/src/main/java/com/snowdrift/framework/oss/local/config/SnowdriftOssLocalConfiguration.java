package com.snowdrift.framework.oss.local.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.local.service.LocalOssServiceImpl;
import com.snowdrift.framework.oss.properties.OssProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 本地存储自动配置类
 *
 * @author 83674
 * @date 2026/5/9
 * @description 自动注册本地存储实例到策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SnowdriftOssLocalConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @PostConstruct
    public void registerLocalOssService() {
        ossStrategyFactory.registerFromProperties(ossProperties, OssTypeEnum.LOCAL,
                LocalOssServiceImpl::new);
    }
}
