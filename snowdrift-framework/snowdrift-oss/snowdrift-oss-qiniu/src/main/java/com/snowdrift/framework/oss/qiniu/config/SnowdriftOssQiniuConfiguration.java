package com.snowdrift.framework.oss.qiniu.config;

import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.qiniu.service.QiniuOssServiceImpl;
import com.snowdrift.framework.oss.util.OssConfigConverter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

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
@ConditionalOnClass(QiniuOssServiceImpl.class)
public class SnowdriftOssQiniuConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    /**
     * 注册七牛云 OSS 实例
     * <p>
     * 在应用启动时，扫描所有七牛云类型的 OSS 配置并注册到策略工厂
     */
    @PostConstruct
    public void registerQiniuOssService() {
        if (MapUtils.isEmpty(ossProperties.getConfigs())) {
            log.debug("未配置 OSS 实例，跳过七牛云 OSS 注册");
            return;
        }

        for (Map.Entry<String, OssInstanceProperties> entry : ossProperties.getConfigs().entrySet()) {
            String configKey = entry.getKey();
            OssInstanceProperties properties = entry.getValue();

            if (OssTypeEnum.QINIU == properties.getOssType() && properties.getEnabled()) {
                registerQiniuService(configKey, properties);
            }
        }
    }

    /**
     * 注册单个七牛云 OSS 实例
     * <p>
     * 将配置转换为 OssConfigDTO 并创建 QiniuOssServiceImpl 实例
     *
     * @param configKey  配置标识
     * @param properties OSS 实例配置
     */
    private void registerQiniuService(String configKey, OssInstanceProperties properties) {
        try {
            OssConfigDTO config = OssConfigConverter.fromProperties(properties, configKey);
            // 注册服务
            ossStrategyFactory.register(configKey, new QiniuOssServiceImpl(config));

            log.info("七牛云 OSS 实例注册成功: configKey={}, domain={}, bucket={}",
                    configKey, properties.getDomain(),properties.getBucket());
        } catch (Exception e) {
            log.error("七牛云 OSS 实例注册失败: configKey={}", configKey, e);
        }
    }
}
