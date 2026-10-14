package com.snowdrift.social.dingtalk.service;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.config.DingTalkConfigSnapshot;
import com.snowdrift.social.dingtalk.config.DingTalkServiceConfig;
import com.snowdrift.social.dingtalk.config.IDingTalkConfigProvider;
import org.junit.jupiter.api.Test;

import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenResponseBody;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenResponse;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenRequest;
import com.snowdrift.social.dingtalk.utils.ClientUtil;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DingTalkServiceRegistryTest {

    @Test
    void routesActiveAndNamedCallsToConfiguredStrategies() {
        TestConfigProvider provider = new TestConfigProvider(snapshot("first", config("first", "app-1"),
                config("second", "app-2")));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);

        assertThat(registry.getActiveService().getUserOauthUrl(null)).contains("client_id=app-1");
        assertThat(registry.getService("second").getUserOauthUrl(null)).contains("client_id=app-2");
    }

    @Test
    void activationUpdatesTheProviderAndActiveStrategy() {
        TestConfigProvider provider = new TestConfigProvider(snapshot("first", config("first", "app-1"),
                config("second", "app-2")));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);

        registry.activate("second");

        assertThat(provider.activatedKey).isEqualTo("second");
        assertThat(registry.getActiveConfigKey()).isEqualTo("second");
        assertThat(registry.getActiveService().getUserOauthUrl(null)).contains("client_id=app-2");
    }

    @Test
    void invalidRefreshKeepsPreviousValidSnapshot() {
        TestConfigProvider provider = new TestConfigProvider(snapshot("first", config("first", "app-1")));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);

        assertThatThrownBy(() -> registry.refresh(snapshot("missing", config("first", "app-1"))))
                .isInstanceOf(BizException.class);

        assertThat(registry.getActiveConfigKey()).isEqualTo("first");
        assertThat(registry.getActiveService().getUserOauthUrl(null)).contains("client_id=app-1");
    }

    @Test
    void disabledAndMissingStrategiesCannotBeSelected() {
        DingTalkServiceConfig disabled = new DingTalkServiceConfig("disabled", "app", "secret", null, false);
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(
                new TestConfigProvider(snapshot("active", config("active", "app-1"), disabled)));

        assertThatThrownBy(() -> registry.getService("disabled")).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> registry.activate("missing")).isInstanceOf(BizException.class);
    }

    @Test
    void rejectsInvalidEnabledConfigWithoutPublishingPartialState() {
        TestConfigProvider provider = new TestConfigProvider(snapshot("first", config("first", "app-1")));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);
        DingTalkServiceConfig invalid = new DingTalkServiceConfig("second", "", "secret", null, true);

        assertThatThrownBy(() -> registry.refresh(snapshot("second", config("first", "app-1"), invalid)))
                .isInstanceOf(BizException.class);

        assertThat(registry.getActiveConfigKey()).isEqualTo("first");
    }

    private static DingTalkServiceConfig config(String key, String appKey) {
        return new DingTalkServiceConfig(key, appKey, "secret", "https://example.com/callback", true);
    }

    @Test
    void emptySnapshotAllowsRegistryButFacadeFailsClearly() {
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(new TestConfigProvider(snapshot(null)));
        IDingTalkService facade = new ActiveDingTalkService(registry);
        assertThat(registry.getActiveConfigKey()).isNull();
        assertThatThrownBy(facade::getAppAccessToken).hasMessage("当前没有激活的钉钉服务配置");
        assertThatThrownBy(() -> facade.getUserOauthUrl(null)).hasMessage("当前没有激活的钉钉服务配置");
        assertThatThrownBy(() -> facade.getUserAccessToken("code")).hasMessage("当前没有激活的钉钉服务配置");
    }

    @Test
    void providerFailuresKeepThePreviouslyPublishedState() {
        IDingTalkConfigProvider provider = mock(IDingTalkConfigProvider.class);
        when(provider.load()).thenReturn(snapshot("first", config("first", "app-1"), config("second", "app-2")));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);
        RuntimeException failure = new IllegalStateException("storage unavailable");
        when(provider.load()).thenThrow(failure);
        assertThatThrownBy(registry::refresh).hasMessage("加载钉钉配置失败").hasCause(failure);
        doThrow(failure).when(provider).activate("second");
        assertThatThrownBy(() -> registry.activate("second")).hasMessage("激活钉钉配置失败: second").hasCause(failure);
        assertThat(registry.getActiveConfigKey()).isEqualTo("first");
        assertThat(registry.getActiveService().getUserOauthUrl(null)).contains("client_id=app-1");
    }

    @Test
    void namedOauthFlowStaysOnOriginalApplicationAfterActivation() throws Exception {
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(new TestConfigProvider(
                snapshot("first", config("first", "app-1"), config("second", "app-2"))));
        assertThat(registry.getService("first").getUserOauthUrl("state")).contains("client_id=app-1");
        registry.activate("second");
        Client client = mock(Client.class);
        when(client.getUserToken(any())).thenReturn(
                new GetUserTokenResponse().setStatusCode(200).setBody(
                        new GetUserTokenResponseBody().setAccessToken("token").setExpireIn(3600L)));
        try (MockedStatic<ClientUtil> clients =
                     mockStatic(ClientUtil.class)) {
            clients.when(() -> ClientUtil.getInstance(Client.class)).thenReturn(client);
            registry.getService("first").getUserAccessToken("code");
            ArgumentCaptor<GetUserTokenRequest> request =
                    ArgumentCaptor.forClass(GetUserTokenRequest.class);
            verify(client).getUserToken(request.capture());
            assertThat(request.getValue().getClientId()).isEqualTo("app-1");
        }
        assertThat(new ActiveDingTalkService(registry).getUserOauthUrl(null)).contains("client_id=app-2");
    }

    @Test
    void concurrentRefreshAndActiveReadsUseACompleteSnapshot() throws Exception {
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(new TestConfigProvider(snapshot("first", config("first", "app-1"))));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> writer = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 20000; i++) {
                    String key = i % 2 == 0 ? "first" : "second";
                    registry.refresh(snapshot(key, config(key, key)));
                }
            });
            Future<?> reader = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 100000; i++) {
                    assertThat(registry.getActiveService()).isNotNull();
                }
            });
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent test did not start");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static DingTalkConfigSnapshot snapshot(String activeKey, DingTalkServiceConfig... configs) {
        Map<String, DingTalkServiceConfig> configMap = new LinkedHashMap<>();
        for (DingTalkServiceConfig config : configs) {
            configMap.put(config.getConfigKey(), config);
        }
        return new DingTalkConfigSnapshot(activeKey, configMap);
    }

    private static final class TestConfigProvider implements IDingTalkConfigProvider {
        private final DingTalkConfigSnapshot snapshot;
        private String activatedKey;

        private TestConfigProvider(DingTalkConfigSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public DingTalkConfigSnapshot load() {
            return snapshot;
        }

        @Override
        public void activate(String configKey) {
            activatedKey = configKey;
        }
    }
}
