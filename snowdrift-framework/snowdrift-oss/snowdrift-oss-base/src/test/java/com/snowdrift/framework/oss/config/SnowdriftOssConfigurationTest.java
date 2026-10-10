package com.snowdrift.framework.oss.config;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import com.snowdrift.framework.oss.properties.OssProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftOssConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 自动配置黑盒测试：Bean 方法收集 registrations 并按属性注册实例、设置默认 key；
 * ApplicationContextRunner 验证条件装配与 @ConditionalOnMissingBean 让位
 * @since 1.0.0
 */
class SnowdriftOssConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftOssConfiguration.class));

    private OssInstanceProperties localInstance() {
        OssInstanceProperties instance = new OssInstanceProperties();
        instance.setOssType(OssTypeEnum.LOCAL);
        instance.setEnabled(true);
        instance.setEndpoint(System.getProperty("java.io.tmpdir"));
        instance.setBucket("bkt");
        return instance;
    }

    @Test
    @DisplayName("Bean 方法：设置默认 key，并按 registrations 从属性注册匹配类型的实例")
    void beanMethodRegistersFromProperties() {
        OssProperties properties = new OssProperties();
        properties.setDefaultConfigKey("myDefault");
        properties.setConfigs(Map.of("local1", localInstance()));

        OssServiceRegistration registration =
                new OssServiceRegistration(OssTypeEnum.LOCAL, config -> mock(IOssService.class));

        OssStrategyFactory factory =
                new SnowdriftOssConfiguration().ossStrategyFactory(properties, List.of(registration));

        assertThat(factory.getDefaultConfigKey()).isEqualTo("myDefault");
        assertThat(factory.contains("local1")).isTrue();
    }

    @Test
    @DisplayName("Bean 方法：无 registration 时工厂仍创建，默认 key 生效，无实例注册")
    void beanMethodWithoutRegistrations() {
        OssProperties properties = new OssProperties();
        properties.setDefaultConfigKey("d");

        OssStrategyFactory factory =
                new SnowdriftOssConfiguration().ossStrategyFactory(properties, List.of());

        assertThat(factory.getDefaultConfigKey()).isEqualTo("d");
        assertThat(factory.getAllServices()).isEmpty();
    }

    @Test
    @DisplayName("自动配置：绑定 default-config-key 并注册 OssStrategyFactory Bean")
    void autoConfigurationBindsProperties() {
        runner.withPropertyValues("snowdrift.oss.default-config-key=abc")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OssStrategyFactory.class);
                    assertThat(ctx.getBean(OssStrategyFactory.class).getDefaultConfigKey()).isEqualTo("abc");
                });
    }

    @Test
    @DisplayName("已存在用户自定义 OssStrategyFactory 时让位（@ConditionalOnMissingBean）")
    void backsOffWhenUserBeanPresent() {
        runner.withPropertyValues("snowdrift.oss.default-config-key=abc")
                .withBean(OssStrategyFactory.class, OssStrategyFactory::new)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OssStrategyFactory.class);
                    // 用户 Bean 未被自动配置覆盖：defaultConfigKey 保持字段默认值 "default"（而非属性 abc）
                    assertThat(ctx.getBean(OssStrategyFactory.class).getDefaultConfigKey()).isEqualTo("default");
                });
    }
}
