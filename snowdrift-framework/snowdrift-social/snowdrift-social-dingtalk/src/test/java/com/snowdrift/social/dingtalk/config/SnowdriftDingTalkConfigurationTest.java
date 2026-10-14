package com.snowdrift.social.dingtalk.config;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.event.IEventHandler;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import com.snowdrift.social.dingtalk.service.DingTalkServiceRegistry;
import com.snowdrift.social.dingtalk.service.IDingTalkService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SnowdriftDingTalkConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftDingTalkConfiguration.class))
            .withPropertyValues(
                    "snowdrift.social.dingtalk.enabled=true",
                    "snowdrift.social.dingtalk.active-config-key=primary",
                    "snowdrift.social.dingtalk.configs.primary.app-key=test-app",
                    "snowdrift.social.dingtalk.configs.primary.app-secret=test-secret",
                    "snowdrift.social.dingtalk.configs.primary.oauth-redirect-uri=https://example.com/callback"
            );

    @Test
    void registersDefaultProviderRegistryAndStableServiceFacade() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IDingTalkConfigProvider.class);
            assertThat(context).hasSingleBean(DingTalkServiceRegistry.class);
            assertThat(context).hasSingleBean(IDingTalkService.class);
            assertThat(context.getBean(DingTalkServiceRegistry.class).getActiveConfigKey()).isEqualTo("primary");
        });
    }

    @Test
    void leavesDingTalkDisabledUnlessExplicitlyEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftDingTalkConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IDingTalkService.class);
                    assertThat(context).doesNotHaveBean(DingTalkServiceRegistry.class);
                });
    }

    private ApplicationContextRunner emptyRunner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SnowdriftDingTalkConfiguration.class))
                .withPropertyValues("snowdrift.social.dingtalk.enabled=true");
    }

    private ApplicationContextRunner streamRunner() {
        return emptyRunner().withPropertyValues("snowdrift.social.dingtalk.event-listener.enabled=true",
                "snowdrift.social.dingtalk.event-listener.client-id=test-client",
                "snowdrift.social.dingtalk.event-listener.client-secret=test-secret");
    }

    private IEventHandler handler() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("test-event");
        return handler;
    }

    @Test
    void emptyConfigurationStartsWithAnInactiveFacade() {
        emptyRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class).hasSingleBean(DingTalkServiceRegistry.class);
            assertThatThrownBy(() -> context.getBean(IDingTalkService.class).getAppAccessToken())
                    .hasMessage("当前没有激活的钉钉服务配置");
        });
    }

    @Test
    void rejectsMissingHandlersAndCredentialsBeforeSdkCreation() {
        streamRunner().run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("启用钉钉 Stream 时必须提供 IEventHandler Bean");
        });
        emptyRunner().withPropertyValues("snowdrift.social.dingtalk.event-listener.enabled=true")
                .withBean(IEventHandler.class, this::handler)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉流监听配置的 clientId 不能为空");
                });
        streamRunner().withPropertyValues("snowdrift.social.dingtalk.event-listener.client-secret=")
                .withBean(IEventHandler.class, this::handler)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉流监听配置的 clientSecret 不能为空");
                });
    }

    @Test
    void defaultProviderYieldsToCustomProvider() {
        IDingTalkConfigProvider provider = mock(IDingTalkConfigProvider.class);
        when(provider.load()).thenReturn(new DingTalkConfigSnapshot("database", Map.of("database",
                new DingTalkServiceConfig("database", "db-app", "db-secret", null, true))));
        contextRunner.withBean(IDingTalkConfigProvider.class, () -> provider).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IDingTalkConfigProvider.class);
            assertThat(context.getBean(IDingTalkConfigProvider.class)).isSameAs(provider);
            assertThat(context.getBean(DingTalkServiceRegistry.class).getActiveConfigKey()).isEqualTo("database");
        });
    }

    @Test
    void customServiceAndRegistryOverrideDefaults() {
        IDingTalkService service = mock(IDingTalkService.class);
        emptyRunner().withBean(IDingTalkService.class, () -> service).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class).doesNotHaveBean(DingTalkServiceRegistry.class);
            assertThat(context.getBean(IDingTalkService.class)).isSameAs(service);
        });
        IDingTalkConfigProvider provider = mock(IDingTalkConfigProvider.class);
        when(provider.load()).thenReturn(new DingTalkConfigSnapshot(null, Map.of()));
        DingTalkServiceRegistry registry = new DingTalkServiceRegistry(provider);
        emptyRunner().withBean(DingTalkServiceRegistry.class, () -> registry).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(DingTalkServiceRegistry.class).hasSingleBean(IDingTalkService.class);
            assertThat(context.getBean(DingTalkServiceRegistry.class)).isSameAs(registry);
        });
    }

    @Test
    void customStreamClientIsNotStartedOrStoppedByAutoConfiguration() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        streamRunner().withBean(OpenDingTalkClient.class, () -> client).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OpenDingTalkClient.class);
            context.getBean(SnowdriftDingTalkConfiguration.class).close();
        });
        verifyNoInteractions(client);
    }

    @Test
    void streamOnlyStartsAndContextShutdownStopsExactlyOnce() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        java.util.concurrent.atomic.AtomicReference<SnowdriftDingTalkConfiguration> configuration = new java.util.concurrent.atomic.AtomicReference<>();
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            streamRunner().withBean(IEventHandler.class, this::handler).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(OpenDingTalkClient.class).hasSingleBean(IDingTalkService.class);
                assertThat(context.getBean(DingTalkServiceRegistry.class).getActiveConfigKey()).isNull();
                configuration.set(context.getBean(SnowdriftDingTalkConfiguration.class));
            });
            configuration.get().close();
            verify(client).start();
            verify(client).stop();
        }
    }

    @Test
    void legacySingleConfigIsBoundAndInvalidConfiguredAppsStillFail() {
        emptyRunner().withPropertyValues("snowdrift.social.dingtalk.app-key=legacy-app",
                "snowdrift.social.dingtalk.app-secret=legacy-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DingTalkServiceRegistry.class).getActiveConfigKey()).isEqualTo("default");
                });
        emptyRunner().withPropertyValues("snowdrift.social.dingtalk.app-key=legacy-app")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉 appSecret 不能为空: configKey=default");
                });
        contextRunner.withPropertyValues("snowdrift.social.dingtalk.configs.primary.enabled=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("当前激活的钉钉配置不存在或未启用: primary");
                });
    }

    @Test
    void failedStartCleansUpAndPreservesSuppressedStopFailure() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        Exception startFailure = new Exception("start failed");
        Exception stopFailure = new Exception("stop failed");
        doThrow(startFailure).when(client).start();
        doThrow(stopFailure).when(client).stop();
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        SnowdriftDingTalkConfiguration configuration = new SnowdriftDingTalkConfiguration();
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            assertThatThrownBy(() -> configuration.openDingTalkClient(streamProperties(), List.of(handler())))
                    .isInstanceOf(BizException.class).hasMessage("钉钉 Stream 客户端启动失败").hasCause(startFailure);
            assertThat(startFailure.getSuppressed()).containsExactly(stopFailure);
            configuration.close();
            verify(client).stop();
        }
    }

    @Test
    void repeatedCloseDoesNotRetryEvenWhenStopFails() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        Exception stopFailure = new Exception("stop failed");
        doThrow(stopFailure).when(client).stop();
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        SnowdriftDingTalkConfiguration configuration = new SnowdriftDingTalkConfiguration();
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            configuration.openDingTalkClient(streamProperties(), List.of(handler()));
            assertThatThrownBy(configuration::close).hasMessage("钉钉 Stream 客户端停止失败").hasCause(stopFailure);
            configuration.close();
            verify(client).stop();
        }
    }

    private DingTalkSocialProperties streamProperties() {
        DingTalkSocialProperties properties = new DingTalkSocialProperties();
        DingTalkSocialProperties.EventListenerProperties stream = new DingTalkSocialProperties.EventListenerProperties();
        stream.setClientId("test-client");
        stream.setClientSecret("test-secret");
        properties.setEventListener(stream);
        return properties;
    }
}
