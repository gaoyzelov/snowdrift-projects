package com.snowdrift.framework.cache.config;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.IDistributedLockService;
import com.snowdrift.framework.cache.aspect.DistributedLockAspect;
import com.snowdrift.framework.cache.aspect.RepeatSubmitAspect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftCacheAspectBackoffWarningTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 缺后端时 @DistributedLock / @RepeatSubmit 会静默退化为空操作，必须留下 WARN 而不是无声失效
 * @since 1.0.0
 */
@ExtendWith(OutputCaptureExtension.class)
class SnowdriftCacheAspectBackoffWarningTest {

    private static final String LOCK_MARKER = "@DistributedLock 将退化为空操作";
    private static final String REPEAT_MARKER = "@RepeatSubmit 将退化为空操作";

    /**
     * 带上 Boot 的 {@code CacheAutoConfiguration}：@EnableCaching 需要容器里有 CacheManager，
     * 真实应用里由它提供（无后端时退化为 ConcurrentMapCacheManager），此处保持一致以免测出假失败。
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CacheAutoConfiguration.class,
                    SnowdriftCacheConfiguration.class));

    @Test
    @DisplayName("既无 IDistributedLockService 也无 ICacheService：切面不注册，且必须告警说明注解会静默失效")
    void warnsWhenNoBackendPresent(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(DistributedLockAspect.class);
            assertThat(context).doesNotHaveBean(RepeatSubmitAspect.class);

            assertThat(output).contains(LOCK_MARKER);
            assertThat(output).contains(REPEAT_MARKER);
        });
    }

    @Test
    @DisplayName("两个后端都在位、切面正常注册时不告警（避免假警报）")
    void silentWhenBackendsPresent(CapturedOutput output) {
        runner.withUserConfiguration(BackendsConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(DistributedLockAspect.class);
            assertThat(context).hasSingleBean(RepeatSubmitAspect.class);

            assertThat(output).doesNotContain(LOCK_MARKER);
            assertThat(output).doesNotContain(REPEAT_MARKER);
        });
    }

    /** 只为触发 @ConditionalOnBean 的退避条件，不参与断言 */
    @Configuration(proxyBeanMethods = false)
    static class BackendsConfiguration {

        @Bean
        IDistributedLockService distributedLockService() {
            return mock(IDistributedLockService.class);
        }

        @Bean
        ICacheService cacheService() {
            return mock(ICacheService.class);
        }
    }
}
