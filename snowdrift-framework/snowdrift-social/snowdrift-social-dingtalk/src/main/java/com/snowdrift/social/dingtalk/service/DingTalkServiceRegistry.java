package com.snowdrift.social.dingtalk.service;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.config.DingTalkConfigSnapshot;
import com.snowdrift.social.dingtalk.config.DingTalkServiceConfig;
import com.snowdrift.social.dingtalk.config.IDingTalkConfigProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 按配置标识管理钉钉服务策略，并维护当前激活策略。
 */
public class DingTalkServiceRegistry {

    private final IDingTalkConfigProvider configProvider;
    private volatile RegistryState state = new RegistryState(Map.of(), null);

    public DingTalkServiceRegistry(IDingTalkConfigProvider configProvider) {
        if (configProvider == null) {
            throw new BizException("钉钉配置提供器不能为空");
        }
        this.configProvider = configProvider;
        refresh();
    }

    /**
     * 从当前配置源加载完整快照。读取或校验失败时保留上一份有效快照。
     */
    public synchronized void refresh() {
        DingTalkConfigSnapshot snapshot;
        try {
            snapshot = configProvider.load();
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BizException("加载钉钉配置失败", e);
        }
        refresh(snapshot);
    }

    /**
     * 校验并原子替换完整策略快照。
     */
    public synchronized void refresh(DingTalkConfigSnapshot snapshot) {
        if (snapshot == null) {
            throw new BizException("钉钉配置快照不能为空");
        }

        Map<String, IDingTalkService> services = new LinkedHashMap<>();
        Map<String, DingTalkServiceConfig> configs = snapshot.getConfigs();
        for (Map.Entry<String, DingTalkServiceConfig> entry : configs.entrySet()) {
            String configKey = entry.getKey();
            DingTalkServiceConfig config = entry.getValue();
            if (StringUtils.isBlank(configKey)) {
                throw new BizException("钉钉服务配置标识不能为空");
            }
            if (config == null) {
                throw new BizException("钉钉服务配置不能为空: configKey=" + configKey);
            }
            if (!configKey.equals(config.getConfigKey())) {
                throw new BizException("钉钉配置标识与配置内容不一致: configKey=" + configKey);
            }
            if (!config.isEnabled()) {
                continue;
            }
            validate(config);
            services.put(configKey, new IDingTalkServiceImpl(config));
        }

        String activeConfigKey = snapshot.getActiveConfigKey();
        if (services.isEmpty()) {
            if (StringUtils.isNotBlank(activeConfigKey)) {
                throw new BizException("当前激活的钉钉配置不存在或未启用: " + activeConfigKey);
            }
        } else if (StringUtils.isBlank(activeConfigKey) || !services.containsKey(activeConfigKey)) {
            throw new BizException("当前激活的钉钉配置不存在或未启用: " + activeConfigKey);
        }

        // 只在所有条目及激活项均有效后发布新状态，调用方不会观察到半份配置。
        state = new RegistryState(services, activeConfigKey);
    }

    public IDingTalkService getService(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            throw new BizException("钉钉服务配置不存在: " + configKey);
        }
        IDingTalkService service = state.services().get(configKey);
        if (service == null) {
            throw new BizException("钉钉服务配置不存在或未启用: " + configKey);
        }
        return service;
    }

    public IDingTalkService getActiveService() {
        RegistryState current = state;
        String activeConfigKey = current.activeConfigKey();
        if (StringUtils.isBlank(activeConfigKey)) {
            throw new BizException("当前没有激活的钉钉服务配置");
        }
        IDingTalkService service = current.services().get(activeConfigKey);
        if (service == null) {
            throw new BizException("钉钉服务配置不存在或未启用: " + activeConfigKey);
        }
        return service;
    }

    public String getActiveConfigKey() {
        return state.activeConfigKey();
    }

    /**
     * 先更新权威配置源，再切换本实例的活动策略。
     * <p>
     * 多实例部署时，配置源应负责持久化；其他实例仍需通过通知或轮询调用 {@link #refresh()}。
     */
    public synchronized void activate(String configKey) {
        getService(configKey);
        try {
            configProvider.activate(configKey);
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BizException("激活钉钉配置失败: " + configKey, e);
        }
        RegistryState current = state;
        state = new RegistryState(current.services(), configKey);
    }

    private void validate(DingTalkServiceConfig config) {
        if (StringUtils.isBlank(config.getAppKey())) {
            throw new BizException("钉钉 appKey 不能为空: configKey=" + config.getConfigKey());
        }
        if (StringUtils.isBlank(config.getAppSecret())) {
            throw new BizException("钉钉 appSecret 不能为空: configKey=" + config.getConfigKey());
        }
    }

    private record RegistryState(Map<String, IDingTalkService> services, String activeConfigKey) {
        private RegistryState {
            services = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        }
    }
}
