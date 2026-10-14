package com.snowdrift.social.dingtalk.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DingTalkSocialProperties
 * @author gaoyzelov
 * @date 2026/10/13-17:26
 * @description 钉钉社交配置
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snowdrift.social.dingtalk")
public class DingTalkSocialProperties {

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 企业内部应用的 Client ID
     */
    private String appKey;

    /**
     * 企业内部应用的 Client Secret
     */
    @ToString.Exclude
    private String appSecret;

    /**
     * OAuth重定向URI
     */
    private String oauthRedirectUri;

    /**
     * 当前激活的服务配置标识
     */
    private String activeConfigKey = "default";

    /**
     * 多个企业内部应用配置，key 为调用方使用的配置标识
     */
    private Map<String, DingTalkServiceProperties> configs = new LinkedHashMap<>();

    /**
     * Stream事件推送监听器配置
     */
    private EventListenerProperties eventListener;

    @Getter
    @Setter
    public static class EventListenerProperties {

        /**
         * 是否启用
         */
        private Boolean enabled;

        /**
         * 客户端ID
         */
        private String clientId;
        /**
         * 客户端密钥
         */
        private String clientSecret;
    }

    @Getter
    @Setter
    public static class DingTalkServiceProperties {

        /** 是否启用该应用配置 */
        private Boolean enabled = true;

        /** 企业内部应用的 Client ID */
        private String appKey;

        /** 企业内部应用的 Client Secret */
        @ToString.Exclude
        private String appSecret;

        /** OAuth 重定向 URI */
        private String oauthRedirectUri;
    }
}
