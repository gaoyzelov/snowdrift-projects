package com.snowdrift.social.dingtalk.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * DingTalkSocialProperties
 * @author gaoyzelov
 * @date 2026/10/13-17:26
 * @description 钉钉社交配置
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.social.dingtalk")
public class DingTalkSocialProperties implements Serializable {

    /**
     * 基础配置
     */
    private BaseProperties base;

    /**
     * Stream事件推送监听器配置
     */
    private EventListenerProperties eventListener;

    @Data
    public static class EventListenerProperties implements Serializable {

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
        @ToString.Exclude
        private String clientSecret;
    }

    @Data
    public static class BaseProperties implements Serializable {

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
    }
}
