package com.snowdrift.social.dingtalk.config;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 Spring 配置文件构建钉钉服务配置的默认实现。
 */
public class PropertiesDingTalkConfigProvider implements IDingTalkConfigProvider {

    private final DingTalkSocialProperties properties;
    private volatile String activeConfigKey;

    public PropertiesDingTalkConfigProvider(DingTalkSocialProperties properties) {
        if (properties == null) {
            throw new BizException("钉钉配置不能为空");
        }
        this.properties = properties;
        this.activeConfigKey = StringUtils.defaultIfBlank(properties.getActiveConfigKey(), "default");
    }

    @Override
    public DingTalkConfigSnapshot load() {
        Map<String, DingTalkServiceConfig> configs = new LinkedHashMap<>();
        Map<String, DingTalkSocialProperties.DingTalkServiceProperties> configuredInstances = properties.getConfigs();
        if (configuredInstances != null && !configuredInstances.isEmpty()) {
            configuredInstances.forEach((configKey, value) -> {
                if (value == null) {
                    throw new BizException("钉钉服务配置不能为空: configKey=" + configKey);
                }
                configs.put(configKey, new DingTalkServiceConfig(
                        configKey,
                        value.getAppKey(),
                        value.getAppSecret(),
                        value.getOauthRedirectUri(),
                        !Boolean.FALSE.equals(value.getEnabled())
                ));
            });
        } else if (hasLegacyConfiguration()) {
            // 兼容原有单配置写法，将其映射为 default 策略。
            configs.put("default", new DingTalkServiceConfig(
                    "default",
                    properties.getAppKey(),
                    properties.getAppSecret(),
                    properties.getOauthRedirectUri(),
                    true
            ));
        }
        return new DingTalkConfigSnapshot(configs.isEmpty() ? null : activeConfigKey, configs);
    }

    @Override
    public void activate(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            throw new BizException("钉钉服务配置标识不能为空");
        }
        DingTalkServiceConfig config = load().getConfigs().get(configKey);
        if (config == null || !config.isEnabled()) {
            throw new BizException("钉钉服务配置不存在或未启用: " + configKey);
        }
        activeConfigKey = configKey;
    }

    private boolean hasLegacyConfiguration() {
        return StringUtils.isNotBlank(properties.getAppKey())
                || StringUtils.isNotBlank(properties.getAppSecret())
                || StringUtils.isNotBlank(properties.getOauthRedirectUri());
    }
}
