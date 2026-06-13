package com.snowdrift.framework.oss.qiniu.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.qiniu.service.QiniuOssServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 七牛云 OSS 自动配置类
 *
 * @author 83674
 * @date 2026/5/12
 * @description 自动注册七牛云 OSS 实例到策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SnowdriftOssQiniuConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @PostConstruct
    public void registerQiniuOssService() {
        ossStrategyFactory.registerFromProperties(ossProperties, OssTypeEnum.QINIU,
                QiniuOssServiceImpl::new);
    }
}
