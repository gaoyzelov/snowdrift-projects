package com.snowdrift.social.dingtalk.service;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DingTalkServiceFactoryTest {

    private final String key = "factory-test-" + UUID.randomUUID();

    @AfterEach
    void cleanup() {
        DingTalkServiceFactory.remove(key);
    }

    private DingTalkSocialConfig config(String appKey) {
        return new DingTalkSocialConfig(key, appKey, "test-secret", "https://example.com/callback");
    }

    @Test
    void firstCreateReturnsAndRegistersTheInstance() {
        assertThat(DingTalkServiceFactory.get(key)).isNull();
        assertThat(DingTalkServiceFactory.contains(key)).isFalse();
        IDingTalkService service = DingTalkServiceFactory.create(config("app-1"));
        assertThat(service).isNotNull().isSameAs(DingTalkServiceFactory.get(key));
        assertThat(DingTalkServiceFactory.contains(key)).isTrue();
    }

    @Test
    void repeatedCreateReusesTheOriginalConfiguration() {
        IDingTalkService first = DingTalkServiceFactory.create(config("app-1"));
        IDingTalkService second = DingTalkServiceFactory.create(config("app-2"));
        assertThat(second).isSameAs(first);
        assertThat(second.getUserOauthUrl(null)).contains("client_id=app-1");
    }

    @Test
    void differentKeysAreIsolated() {
        String otherKey = key + "-other";
        try {
            IDingTalkService first = DingTalkServiceFactory.create(config("app-1"));
            IDingTalkService other = DingTalkServiceFactory.create(
                    new DingTalkSocialConfig(otherKey, "app-2", "secret-2", "https://example.com/other"));
            assertThat(other).isNotSameAs(first);
            assertThat(first.getUserOauthUrl(null)).contains("client_id=app-1");
            assertThat(other.getUserOauthUrl(null)).contains("client_id=app-2");
        } finally {
            DingTalkServiceFactory.remove(otherKey);
        }
    }

    @Test
    void inputMutationDoesNotChangeRegisteredService() {
        DingTalkSocialConfig config = config("app-1");
        IDingTalkService service = DingTalkServiceFactory.create(config);
        config.setAppKey("changed");
        config.setAppSecret("changed-secret");
        config.setOauthRedirectUri("https://changed.example.com");
        config.setConfigKey("changed-key");
        assertThat(service.getUserOauthUrl(null)).contains("client_id=app-1").doesNotContain("changed");
        assertThat(DingTalkServiceFactory.get(key)).isSameAs(service);
    }

    @Test
    void removalAllowsRecreationAndDoesNotChangeHeldReferences() {
        IDingTalkService old = DingTalkServiceFactory.create(config("app-1"));
        assertThat(DingTalkServiceFactory.remove(key)).isSameAs(old);
        assertThat(DingTalkServiceFactory.get(key)).isNull();
        assertThat(DingTalkServiceFactory.contains(key)).isFalse();
        assertThat(DingTalkServiceFactory.remove(key)).isNull();
        IDingTalkService replacement = DingTalkServiceFactory.create(config("app-2"));
        assertThat(replacement).isNotSameAs(old);
        assertThat(replacement.getUserOauthUrl(null)).contains("client_id=app-2");
        assertThat(old.getUserOauthUrl(null)).contains("client_id=app-1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void blankKeysAreRejectedConsistently(String configKey) {
        DingTalkSocialConfig config = config("app");
        config.setConfigKey(configKey);
        assertThatThrownBy(() -> DingTalkServiceFactory.create(config)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.reload(config)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.get(configKey)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.contains(configKey)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.remove(configKey)).isInstanceOf(BizException.class);
    }

    @Test
    void failedCreationDoesNotReserveTheKey() {
        assertThatThrownBy(() -> DingTalkServiceFactory.create(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.create(config(" "))).isInstanceOf(BizException.class);
        assertThat(DingTalkServiceFactory.contains(key)).isFalse();
        assertThat(DingTalkServiceFactory.create(config("valid"))).isNotNull();
    }

    @Test
    void reloadRegistersAnAbsentKey() {
        IDingTalkService service = DingTalkServiceFactory.reload(config("app-1"));
        assertThat(service).isNotNull().isSameAs(DingTalkServiceFactory.get(key));
        assertThat(DingTalkServiceFactory.contains(key)).isTrue();
    }

    @Test
    void reloadReplacesTheMappingAndKeepsOldReferencesUsable() {
        IDingTalkService old = DingTalkServiceFactory.create(config("app-1"));
        DingTalkSocialConfig updatedConfig = config("app-2");
        IDingTalkService replacement = DingTalkServiceFactory.reload(updatedConfig);
        updatedConfig.setAppKey("changed");
        assertThat(replacement).isNotSameAs(old).isSameAs(DingTalkServiceFactory.get(key));
        assertThat(replacement.getUserOauthUrl(null)).contains("client_id=app-2");
        assertThat(old.getUserOauthUrl(null)).contains("client_id=app-1");
        assertThat(DingTalkServiceFactory.create(config("app-3"))).isSameAs(replacement);
    }

    @Test
    void invalidReloadKeepsTheExistingInstance() {
        IDingTalkService old = DingTalkServiceFactory.create(config("app-1"));
        assertThatThrownBy(() -> DingTalkServiceFactory.reload(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> DingTalkServiceFactory.reload(config(" "))).isInstanceOf(BizException.class);
        DingTalkSocialConfig missingSecret = config("app-2");
        missingSecret.setAppSecret(null);
        assertThatThrownBy(() -> DingTalkServiceFactory.reload(missingSecret)).isInstanceOf(BizException.class);
        assertThat(DingTalkServiceFactory.get(key)).isSameAs(old);
        assertThat(old.getUserOauthUrl(null)).contains("client_id=app-1");
    }

    @Test
    void concurrentReloadsKeepOldReadableAndLastPublicationWins() throws Exception {
        IDingTalkService old = DingTalkServiceFactory.create(config("original"));
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        DingTalkSocialConfig slowConfig = mock(DingTalkSocialConfig.class);
        when(slowConfig.getConfigKey()).thenReturn(key);
        when(slowConfig.getAppKey()).thenReturn("slow-app");
        when(slowConfig.getOauthRedirectUri()).thenReturn("https://example.com/callback");
        when(slowConfig.getAppSecret()).thenAnswer(invocation -> {
            building.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Reload construction was not released");
            }
            return "secret";
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<IDingTalkService> slow = executor.submit(() -> DingTalkServiceFactory.reload(slowConfig));
            assertThat(building.await(5, TimeUnit.SECONDS)).isTrue();
            // 新实例构建期间，读取和其他更新均可继续，旧映射始终保留。
            assertThat(DingTalkServiceFactory.get(key)).isSameAs(old);
            Future<IDingTalkService> fast = executor.submit(() -> DingTalkServiceFactory.reload(config("fast-app")));
            IDingTalkService firstPublished = fast.get(5, TimeUnit.SECONDS);
            assertThat(DingTalkServiceFactory.get(key)).isSameAs(firstPublished);
            release.countDown();
            IDingTalkService lastPublished = slow.get(5, TimeUnit.SECONDS);
            assertThat(DingTalkServiceFactory.get(key)).isSameAs(lastPublished);
            assertThat(lastPublished.getUserOauthUrl(null)).contains("client_id=slow-app");
            assertThat(firstPublished.getUserOauthUrl(null)).contains("client_id=fast-app");
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentCreationReturnsTheSameInstance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<IDingTalkService>> results = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                results.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Factory test did not start");
                    }
                    return DingTalkServiceFactory.create(config("app"));
                }));
            }
            start.countDown();
            IDingTalkService first = results.get(0).get(10, TimeUnit.SECONDS);
            assertThat(first).isNotNull();
            for (Future<IDingTalkService> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isSameAs(first);
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
