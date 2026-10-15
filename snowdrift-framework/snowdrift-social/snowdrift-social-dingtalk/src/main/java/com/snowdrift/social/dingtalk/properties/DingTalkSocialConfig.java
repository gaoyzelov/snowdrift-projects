package com.snowdrift.social.dingtalk.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * DingTalkSocialConfig
 *
 * @author gaoyzelov
 * @date 2026/10/15-11:56
 * @description 钉钉社交配置
 * @since 1.0.0
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class DingTalkSocialConfig implements Serializable {

    /**
     * 配置标识
     */
    private String configKey;

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
