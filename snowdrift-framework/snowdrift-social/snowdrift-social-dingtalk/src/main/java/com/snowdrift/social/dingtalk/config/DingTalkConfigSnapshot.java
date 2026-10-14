package com.snowdrift.social.dingtalk.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置源返回的完整、不可变配置快照。
 */
public final class DingTalkConfigSnapshot {

    private final String activeConfigKey;
    private final Map<String, DingTalkServiceConfig> configs;

    public DingTalkConfigSnapshot(String activeConfigKey, Map<String, DingTalkServiceConfig> configs) {
        this.activeConfigKey = activeConfigKey;
        this.configs = configs == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(configs));
    }

    public String getActiveConfigKey() {
        return activeConfigKey;
    }

    public Map<String, DingTalkServiceConfig> getConfigs() {
        return configs;
    }
}
