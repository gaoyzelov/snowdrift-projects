package com.snowdrift.framework.cache.config;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftCacheAspectBackoffWarningTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 缺后端时仍注册切面，注解方法运行时必须 fail-closed，不能静默放行业务
 * @since 1.0.0
 */
@ExtendWith(OutputCaptureExtension.class)
class SnowdriftCacheAspectBackoffWarningTest {

    private static final String LOCK_MARKER = "@DistributedLock 将退化为空操作";
    private static final String REPEAT_MARKER = "@RepeatSubmit 将退化为空操作";

    /** 带上 Boot 的 CacheAutoConfiguration，模拟真实应用中的 CacheManager 装配。 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CacheAutoConfiguration.class,
                    SnowdriftCacheConfiguration.class));

    @Test
    @DisplayName("既无 IDistributedLockService 也无 ICacheService：切面仍注册，且不再输出退化为空操作告警")
    void aspectsRemainRegisteredWhenNoBackendPresent(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DistributedLockAspect.class);
            assertThat(context).hasSingleBean(RepeatSubmitAspect.class);

            assertThat(output).doesNotContain(LOCK_MARKER);
            assertThat(output).doesNotContain(REPEAT_MARKER);
        });
    }
}
