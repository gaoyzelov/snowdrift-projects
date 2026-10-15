package com.snowdrift.social.dingtalk.utils;

import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;

/**
 * DingTalkConfigUtil
 * @author 83674
 * @date 2026/10/15-09:47
 * @description 钉钉社交配置工具类
 * @since 1.0.0
 */
public final class DingTalkConfigUtil {

    private DingTalkConfigUtil() {
    }

    /**
     * 从属性中获取配置
     * @param properties 属性
     * @return 钉钉社交配置
     */
    public static DingTalkSocialConfig fromProperties(DingTalkSocialProperties properties) {
        AssertUtil.notNull(properties, "钉钉社交配置不能为空");
        DingTalkSocialProperties.BaseProperties base = properties.getBase();
        AssertUtil.notNull(base, "钉钉基础配置不能为空");
        DingTalkSocialConfig config = new DingTalkSocialConfig();
        config.setConfigKey("default");
        config.setAppKey(base.getAppKey());
        config.setAppSecret(base.getAppSecret());
        config.setOauthRedirectUri(base.getOauthRedirectUri());
        return config;
    }
}
