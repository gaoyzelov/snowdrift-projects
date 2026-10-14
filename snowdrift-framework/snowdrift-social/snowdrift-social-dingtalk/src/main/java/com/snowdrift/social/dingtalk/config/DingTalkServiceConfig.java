package com.snowdrift.social.dingtalk.config;

/**
 * 不可变的钉钉企业内部应用配置。
 */
public final class DingTalkServiceConfig {

    private final String configKey;
    private final String appKey;
    private final String appSecret;
    private final String oauthRedirectUri;
    private final boolean enabled;

    public DingTalkServiceConfig(String configKey, String appKey, String appSecret,
                                 String oauthRedirectUri, boolean enabled) {
        this.configKey = configKey;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.oauthRedirectUri = oauthRedirectUri;
        this.enabled = enabled;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getOauthRedirectUri() {
        return oauthRedirectUri;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "DingTalkServiceConfig{" +
                "configKey='" + configKey + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
