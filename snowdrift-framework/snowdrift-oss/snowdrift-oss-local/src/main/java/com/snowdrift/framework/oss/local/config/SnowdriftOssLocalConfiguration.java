package com.snowdrift.framework.oss.local.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.local.service.LocalOssServiceImpl;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.util.OssConfigConverter;
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

    /**
     * 初始化时注册本地存储实例
     */
    @PostConstruct
    public void registerLocalOssService() {
        if (ossProperties.getConfigs() == null || ossProperties.getConfigs().isEmpty()) {
            log.debug("未配置 OSS 实例，跳过本地存储注册");
            return;
        }

        // 遍历所有配置，注册本地存储类型
        for (var entry : ossProperties.getConfigs().entrySet()) {
            String configKey = entry.getKey();
            OssInstanceProperties properties = entry.getValue();

            // 只注册本地存储类型
            if (OssTypeEnum.LOCAL == properties.getOssType()) {
                registerLocalService(configKey, properties);
            }
        }
    }

    /**
     * 注册本地存储实例
     */
    private void registerLocalService(String configKey, OssInstanceProperties properties) {
        try {
            // 转换为 DTO
            var config = OssConfigConverter.fromProperties(properties, configKey);
            // 创建并注册 Service
            LocalOssServiceImpl service = new LocalOssServiceImpl(config);
            ossStrategyFactory.register(configKey, service);
            log.info("本地存储实例注册成功: configKey={}, endpoint={}", configKey, properties.getEndpoint());
        } catch (Exception e) {
            log.error("本地存储实例注册失败: configKey={}", configKey, e);
        }
    }
}
