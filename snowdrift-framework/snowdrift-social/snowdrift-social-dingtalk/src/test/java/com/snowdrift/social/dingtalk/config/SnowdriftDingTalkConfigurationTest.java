package com.snowdrift.social.dingtalk.config;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.snowdrift.social.dingtalk.event.IEventHandler;
import com.snowdrift.social.dingtalk.service.DingTalkServiceFactory;
import com.snowdrift.social.dingtalk.service.IDingTalkService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnowdriftDingTalkConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftDingTalkConfiguration.class));

    private ApplicationContextRunner baseRunner() {
        return runner.withPropertyValues("snowdrift.social.dingtalk.base.enabled=true",
                "snowdrift.social.dingtalk.base.app-key=test-app",
                "snowdrift.social.dingtalk.base.app-secret=test-secret",
                "snowdrift.social.dingtalk.base.oauth-redirect-uri=https://example.com/callback");
    }

    private ApplicationContextRunner streamRunner() {
        return runner.withPropertyValues("snowdrift.social.dingtalk.event-listener.enabled=true",
                "snowdrift.social.dingtalk.event-listener.client-id=test-client",
                "snowdrift.social.dingtalk.event-listener.client-secret=test-secret");
    }

    private IEventHandler handler(String type) {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn(type);
        return handler;
    }

    @Test
    void bothCapabilitiesAreDisabledByDefault() {
        runner.run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(IDingTalkService.class).doesNotHaveBean(OpenDingTalkClient.class)
                .doesNotHaveBean(DingTalkServiceFactory.class));
        baseRunner().withPropertyValues("snowdrift.social.dingtalk.base.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(IDingTalkService.class));
    }

    @Test
    void baseConfigurationCreatesAnIndependentSpringService() {
        baseRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class)
                    .doesNotHaveBean(OpenDingTalkClient.class).doesNotHaveBean(DingTalkServiceFactory.class);
            assertThat(context.getBean(IDingTalkService.class).getUserOauthUrl(null)).contains("client_id=test-app");
        });
    }

    @Test
    void redirectIsOptionalUntilAnOauthUrlIsRequested() {
        baseRunner().withPropertyValues("snowdrift.social.dingtalk.base.oauth-redirect-uri=")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class);
                    assertThatThrownBy(() -> context.getBean(IDingTalkService.class).getUserOauthUrl(null))
                            .hasMessage("钉钉 OAuth 回调地址有误");
                });
    }

    @Test
    void enabledBaseRejectsMissingCredentials() {
        runner.withPropertyValues("snowdrift.social.dingtalk.base.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉appKey不能为空");
        });
        baseRunner().withPropertyValues("snowdrift.social.dingtalk.base.app-secret=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉appSecret不能为空");
        });
    }

    @Test
    void customServiceOverridesTheDefaultEvenWithoutBaseCredentials() {
        IDingTalkService service = mock(IDingTalkService.class);
        runner.withPropertyValues("snowdrift.social.dingtalk.base.enabled=true")
                .withBean(IDingTalkService.class, () -> service).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class);
                    assertThat(context.getBean(IDingTalkService.class)).isSameAs(service);
                });
    }

    @Test
    void streamRejectsMissingHandlersAndCredentials() {
        streamRunner().run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("启用钉钉 Stream 时必须提供 IEventHandler Bean");
        });
        streamRunner().withBean(IEventHandler.class, () -> handler("event"))
                .withPropertyValues("snowdrift.social.dingtalk.event-listener.client-id=").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉流监听配置的 clientId 不能为空");
                });
        streamRunner().withBean(IEventHandler.class, () -> handler("event"))
                .withPropertyValues("snowdrift.social.dingtalk.event-listener.client-secret=").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("钉钉流监听配置的 clientSecret 不能为空");
                });
    }

    @Test
    void customStreamClientRetainsItsOwnLifecycle() {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        runner.withPropertyValues("snowdrift.social.dingtalk.event-listener.enabled=true")
                .withBean(OpenDingTalkClient.class, () -> client).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(OpenDingTalkClient.class);
                    context.getBean(SnowdriftDingTalkConfiguration.class).close();
                });
        verifyNoInteractions(client);
    }

    @Test
    void streamStartsWithoutBaseAndStopsExactlyOnce() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        AtomicReference<SnowdriftDingTalkConfiguration> configuration = new AtomicReference<>();
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            streamRunner().withBean(IEventHandler.class, () -> handler("event")).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(OpenDingTalkClient.class).doesNotHaveBean(IDingTalkService.class);
                configuration.set(context.getBean(SnowdriftDingTalkConfiguration.class));
            });
            configuration.get().close();
            verify(client).start();
            verify(client).stop();
        }
    }

    @Test
    void baseAndStreamCanBeEnabledTogether() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            baseRunner().withPropertyValues("snowdrift.social.dingtalk.event-listener.enabled=true",
                            "snowdrift.social.dingtalk.event-listener.client-id=stream-client",
                            "snowdrift.social.dingtalk.event-listener.client-secret=stream-secret")
                    .withBean(IEventHandler.class, () -> handler("event")).run(context ->
                            assertThat(context).hasNotFailed().hasSingleBean(IDingTalkService.class).hasSingleBean(OpenDingTalkClient.class));
            verify(client).start();
            verify(client).stop();
        }
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
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            streamRunner().withBean(IEventHandler.class, () -> handler("event")).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCause(startFailure);
            });
            assertThat(startFailure.getSuppressed()).containsExactly(stopFailure);
            verify(client).stop();
        }
    }

    @Test
    void stopFailureIsWrappedAndRepeatedCloseDoesNotRetry() throws Exception {
        OpenDingTalkClient client = mock(OpenDingTalkClient.class);
        Exception stopFailure = new Exception("stop failed");
        doThrow(stopFailure).when(client).stop();
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(client);
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            streamRunner().withBean(IEventHandler.class, () -> handler("event")).run(context -> {
                assertThat(context).hasNotFailed();
                SnowdriftDingTalkConfiguration configuration = context.getBean(SnowdriftDingTalkConfiguration.class);
                assertThatThrownBy(configuration::close).hasMessage("钉钉 Stream 客户端停止失败").hasCause(stopFailure);
                configuration.close();
            });
            verify(client).stop();
        }
    }

    @Test
    void duplicateHandlersFailBeforeClientCreation() throws Exception {
        OpenDingTalkStreamClientBuilder builder = mock(OpenDingTalkStreamClientBuilder.class, RETURNS_SELF);
        try (MockedStatic<OpenDingTalkStreamClientBuilder> builders = mockStatic(OpenDingTalkStreamClientBuilder.class)) {
            builders.when(OpenDingTalkStreamClientBuilder::custom).thenReturn(builder);
            streamRunner().withBean("first", IEventHandler.class, () -> handler("duplicate"))
                    .withBean("second", IEventHandler.class, () -> handler("duplicate")).run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasStackTraceContaining("重复的钉钉事件处理器");
                    });
            verify(builder, never()).build();
        }
    }
}
