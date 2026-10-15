package com.snowdrift.social.dingtalk.service;

import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DingTalkServiceFactory
 *
 * @author gaoyzelov
 * @date 2026/10/15-11:33
 * @description 钉钉服务工厂，支持自行创建和获取钉钉服务实例
 * @since 1.0.0
 */
public final class DingTalkServiceFactory {

    private DingTalkServiceFactory() {
    }

    /**
     * 钉钉服务映射表
     * Key: 配置标识（如：default、backup）
     * Value: 钉钉服务实例
     */
    private static final Map<String, IDingTalkService> SERVICE_MAP = new ConcurrentHashMap<>();

    /** 获取已创建的实例，不存在时返回 null。 */
    public static IDingTalkService get(String configKey) {
        AssertUtil.notBlank(configKey, "配置标识不能为空");
        return SERVICE_MAP.get(configKey);
    }

    /**
     * 创建并缓存实例。同一标识首次创建生效，后续调用返回已有实例，不覆盖其配置。
     * 配置来源及选择由下游管理；本工厂不加载数据库，也不维护活动应用。
     */
    public static IDingTalkService create(DingTalkSocialConfig config) {
        AssertUtil.notNull(config, "钉钉服务配置不能为空");
        String configKey = config.getConfigKey();
        AssertUtil.notBlank(configKey, "配置标识不能为空");
        return SERVICE_MAP.computeIfAbsent(configKey, key -> new DingTalkServiceImpl(config));
    }

    /**
     * 创建并校验新实例后原子替换映射，返回新实例；标识不存在时直接注册。
     * 创建失败保留旧实例。并发更新以最后发布的实例为准，不比较配置版本。
     * 已持有的旧实例引用仍可使用，更新仅影响后续从工厂获取的实例。
     */
    public static IDingTalkService reload(DingTalkSocialConfig config) {
        AssertUtil.notNull(config, "钉钉服务配置不能为空");
        String configKey = config.getConfigKey();
        AssertUtil.notBlank(configKey, "配置标识不能为空");
        IDingTalkService service = new DingTalkServiceImpl(config);
        SERVICE_MAP.put(configKey, service);
        return service;
    }

    /** 判断是否已创建实例。 */
    public static boolean contains(String configKey) {
        AssertUtil.notBlank(configKey, "配置标识不能为空");
        return SERVICE_MAP.containsKey(configKey);
    }

    /** 移除缓存项并返回旧实例，不存在时返回 null；已持有的实例引用仍可使用。 */
    public static IDingTalkService remove(String configKey) {
        AssertUtil.notBlank(configKey, "配置标识不能为空");
        return SERVICE_MAP.remove(configKey);
    }
}
