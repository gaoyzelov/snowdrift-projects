package com.snowdrift.framework.oss.config;

import com.snowdrift.framework.common.util.AssertUtil;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.properties.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 自动配置类
 *
 * @author 83674
 * @date 2026/5/9
 * @description Spring Boot 自动配置，注册策略工厂
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class SnowdriftOssConfiguration {
    
    /**
     * 注册 OSS 策略工厂
     * <p>
     * 创建并注册 OssStrategyFactory Bean
     * OssStrategyFactory 自带 @PreDestroy 方法，会在应用关闭时自动释放所有 OSS 客户端资源
     */
    @Bean
    @ConditionalOnMissingBean(OssStrategyFactory.class)
    public OssStrategyFactory ossStrategyFactory(OssProperties ossProperties) {
        AssertUtil.notBlank(ossProperties.getDefaultConfigKey(), "OSS默认配置标识不能为空");
        OssStrategyFactory factory = new OssStrategyFactory();
        // 设置默认配置标识
        factory.setDefaultConfigKey(ossProperties.getDefaultConfigKey());
        
        log.info("OSS 策略工厂初始化完成，默认配置: {}", ossProperties.getDefaultConfigKey());
        log.info("已配置的 OSS 实例: {}", ossProperties.getConfigs().keySet());
        
        return factory;
    }
}
