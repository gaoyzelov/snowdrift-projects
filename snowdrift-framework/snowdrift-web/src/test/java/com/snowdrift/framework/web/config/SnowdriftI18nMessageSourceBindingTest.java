package com.snowdrift.framework.web.config;

import com.snowdrift.framework.web.properties.I18nProperties;
import com.snowdrift.framework.web.util.I18nUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

import java.lang.reflect.Field;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SnowdriftI18nMessageSourceBindingTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description I18nUtil 的静态 MessageSource 必须绑定到「容器里最终生效的那个」：
 *              宿主提供 MessageSource 时框架的 {@code messageSource()} 会退避，
 *              若绑定仍只在那个方法里做，就会出现「配了 i18n 却总是回落到 code」
 * @since 1.0.0
 */
@ExtendWith(OutputCaptureExtension.class)
class SnowdriftI18nMessageSourceBindingTest {

    private final SnowdriftI18nConfiguration configuration =
            new SnowdriftI18nConfiguration(new I18nProperties());

    /** I18nUtil 的 MessageSource 是 JVM 全局静态字段，测试前后都重置，避免影响其它用例 */
    @BeforeEach
    @AfterEach
    void resetStaticMessageSource() throws Exception {
        Field field = I18nUtil.class.getDeclaredField("messageSource");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    @DisplayName("binder：把容器里生效的 MessageSource 绑定到 I18nUtil（宿主提供时同样生效）")
    @SuppressWarnings("unchecked")
    void binderBindsEffectiveMessageSource() {
        ObjectProvider<MessageSource> provider = mock(ObjectProvider.class);
        when(provider.getIfUnique()).thenReturn(hostMessageSource());

        configuration.i18nUtilMessageSourceBinder(provider).afterSingletonsInstantiated();

        assertThat(I18nUtil.getMessage("host.key", Locale.US)).isEqualTo("from-host");
    }

    @Test
    @DisplayName("binder：容器中没有唯一 MessageSource 时只告警，不抛异常、不绑定")
    @SuppressWarnings("unchecked")
    void binderToleratesMissingMessageSource(CapturedOutput output) {
        ObjectProvider<MessageSource> provider = mock(ObjectProvider.class);
        when(provider.getIfUnique()).thenReturn(null);

        configuration.i18nUtilMessageSourceBinder(provider).afterSingletonsInstantiated();

        assertThat(output).contains("不存在唯一的 MessageSource");
        // 未绑定 → 回落到 code
        assertThat(I18nUtil.getMessage("host.key", Locale.US)).isEqualTo("host.key");
    }

    @Test
    @DisplayName("无人提供 MessageSource 时框架自身的那个照常注册（既有装配不回退）")
    void frameworkMessageSourceStillRegisters() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftI18nConfiguration.class))
                .withPropertyValues("snowdrift.i18n.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(MessageSource.class));
    }

    private static MessageSource hostMessageSource() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("host.key", Locale.US, "from-host");
        return source;
    }
}
