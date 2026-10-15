package com.snowdrift.social.dingtalk.utils;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DingTalkConfigUtilTest {

    @Test
    void convertsNestedBasePropertiesToADetachedDefaultConfiguration() {
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        DingTalkSocialProperties.BaseProperties base = new DingTalkSocialProperties.BaseProperties();
        base.setAppKey("test-app");
        base.setAppSecret("test-secret");
        base.setOauthRedirectUri("https://example.com/callback");
        properties.setBase(base);
        DingTalkSocialConfig config = DingTalkConfigUtil.fromProperties(properties);
        base.setAppKey("changed");
        assertThat(config.getConfigKey()).isEqualTo("default");
        assertThat(config.getAppKey()).isEqualTo("test-app");
        assertThat(config.getAppSecret()).isEqualTo("test-secret");
        assertThat(config.getOauthRedirectUri()).isEqualTo("https://example.com/callback");
    }

    @Test
    void rejectsMissingPropertiesAndMissingBase() {
        assertThatThrownBy(() -> DingTalkConfigUtil.fromProperties(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkConfigUtil.fromProperties(new DingTalkSocialProperties()))
                .isInstanceOf(BizException.class).hasMessage("钉钉基础配置不能为空");
    }

    @Test
    void configurationToStringExcludesNestedSecrets() {
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        DingTalkSocialProperties.BaseProperties base = new DingTalkSocialProperties.BaseProperties();
        base.setAppKey("app");
        base.setAppSecret("fake-app-secret");
        properties.setBase(base);
        DingTalkSocialProperties.EventListenerProperties listener = new DingTalkSocialProperties.EventListenerProperties();
        listener.setClientSecret("fake-stream-secret");
        properties.setEventListener(listener);
        assertThat(properties.toString()).doesNotContain("fake-app-secret", "fake-stream-secret");
        assertThat(DingTalkConfigUtil.fromProperties(properties).toString()).doesNotContain("fake-app-secret");
    }
}
