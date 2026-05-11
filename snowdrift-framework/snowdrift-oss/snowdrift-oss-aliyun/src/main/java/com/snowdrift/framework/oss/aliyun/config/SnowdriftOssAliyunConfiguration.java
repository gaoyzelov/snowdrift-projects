package com.snowdrift.framework.oss.aliyun.config;

import com.snowdrift.framework.oss.aliyun.service.AliyunOssServiceImpl;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.util.OssConfigConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 阿里云 OSS 自动配置类
 *
 * @author 83674
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

    /**
     * 初始化时注册阿里云 OSS 实例
     */
    @PostConstruct
    public void registerAliyunOssService() {
        if (ossProperties.getConfigs() == null || ossProperties.getConfigs().isEmpty()) {
            log.debug("未配置 OSS 实例，跳过阿里云 OSS 注册");
            return;
        }

        // 遍历所有配置，注册阿里云 OSS 类型
        for (Map.Entry<String, OssInstanceProperties> entry : ossProperties.getConfigs().entrySet()) {
            String configKey = entry.getKey();
            OssInstanceProperties properties = entry.getValue();

            // 只注册阿里云 OSS 类型
            if (OssTypeEnum.ALIYUN == properties.getOssType()) {
                registerAliyunService(configKey, properties);
            }
        }
    }

    /**
     * 注册阿里云 OSS 实例
     *
     * @param configKey  配置标识
     * @param properties 实例配置属性
     */
    private void registerAliyunService(String configKey, OssInstanceProperties properties) {
        try {
            // 转换为 DTO
            OssConfigDTO config = OssConfigConverter.fromProperties(properties, configKey);
            // 创建并注册 Service
            AliyunOssServiceImpl service = new AliyunOssServiceImpl(config);
            ossStrategyFactory.register(configKey, service);

            log.info("阿里云 OSS 实例注册成功: configKey={}, endpoint={}, bucket={}",
                    configKey, properties.getEndpoint(), properties.getBucket());
        } catch (Exception e) {
            log.error("阿里云 OSS 实例注册失败: configKey={}", configKey, e);
        }
    }
}
