package com.snowdrift.social.dingtalk.config;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesDingTalkConfigProviderTest {

    @Test
    void emptyPropertiesProduceAnInactiveEmptySnapshot() {
        DingTalkConfigSnapshot snapshot = new PropertiesDingTalkConfigProvider(new DingTalkSocialProperties()).load();
        assertThat(snapshot.getConfigs()).isEmpty();
        assertThat(snapshot.getActiveConfigKey()).isNull();
    }

    @Test
    void namedConfigurationsTakePrecedenceOverLegacyFields() {
        DingTalkSocialProperties properties = serviceProperties("named-app", "named-secret");
        properties.setAppKey("legacy-app");
        properties.setAppSecret("legacy-secret");
        DingTalkConfigSnapshot snapshot = new PropertiesDingTalkConfigProvider(properties).load();
        assertThat(snapshot.getConfigs().get("default").getAppKey()).isEqualTo("named-app");
    }

    @Test
    void mapsLegacySingleConfigurationToDefaultStrategy() {
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        properties.setAppKey("legacy-app");
        properties.setAppSecret("legacy-secret");
        properties.setOauthRedirectUri("https://example.com/callback");

        DingTalkConfigSnapshot snapshot = new PropertiesDingTalkConfigProvider(properties).load();

        assertThat(snapshot.getActiveConfigKey()).isEqualTo("default");
        assertThat(snapshot.getConfigs()).containsKey("default");
        assertThat(snapshot.getConfigs().get("default").getAppKey()).isEqualTo("legacy-app");
    }

    @Test
    void mapsMultipleConfigurationsAndCanActivateAnother() {
        DingTalkSocialProperties.DingTalkServiceProperties first = serviceConfig("app-1", "secret-1");
        DingTalkSocialProperties.DingTalkServiceProperties second = serviceConfig("app-2", "secret-2");
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        properties.setConfigs(Map.of("first", first, "second", second));
        PropertiesDingTalkConfigProvider provider = new PropertiesDingTalkConfigProvider(properties);

        provider.activate("second");

        assertThat(provider.load().getActiveConfigKey()).isEqualTo("second");
        assertThat(provider.load().getConfigs()).containsKeys("first", "second");
    }

    @Test
    void activationRejectsMissingOrDisabledConfig() {
        DingTalkSocialProperties properties = serviceProperties("app", "secret");
        properties.getConfigs().get("default").setEnabled(false);
        PropertiesDingTalkConfigProvider provider = new PropertiesDingTalkConfigProvider(properties);

        assertThatThrownBy(() -> provider.activate("default")).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> provider.activate("missing")).isInstanceOf(BizException.class);
    }

    private DingTalkSocialProperties serviceProperties(String appKey, String secret) {
        DingTalkSocialProperties.DingTalkServiceProperties config = serviceConfig(appKey, secret);
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        properties.setConfigs(Map.of("default", config));
        return properties;
    }

    private DingTalkSocialProperties.DingTalkServiceProperties serviceConfig(String appKey, String secret) {
        DingTalkSocialProperties.DingTalkServiceProperties config = new DingTalkSocialProperties.DingTalkServiceProperties();
        config.setAppKey(appKey);
        config.setAppSecret(secret);
        config.setOauthRedirectUri("https://example.com/callback");
        return config;
    }
}
