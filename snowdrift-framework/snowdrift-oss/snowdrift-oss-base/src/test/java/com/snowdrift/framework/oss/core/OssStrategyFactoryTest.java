package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import com.snowdrift.framework.oss.properties.OssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * OssStrategyFactoryTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 策略工厂黑盒测试：注册/覆盖关闭旧实例、获取（含 null/未注册）、默认实例、移除关闭、快照隔离、
 * 按配置/属性条件注册、热更新（先移除后注册）、销毁全部关闭且单实例失败不中断
 * @since 1.0.0
 */
class OssStrategyFactoryTest {

    private OssStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OssStrategyFactory();
    }

    private IOssService mockService() {
        IOssService service = mock(IOssService.class);
        when(service.getType()).thenReturn(OssTypeEnum.LOCAL);
        when(service.getBucket()).thenReturn("bkt");
        return service;
    }

    private OssConfigDTO config(String key, boolean enabled, boolean isDefault) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey(key);
        config.setOssType(OssTypeEnum.LOCAL);
        config.setEnabled(enabled);
        config.setIsDefault(isDefault);
        return config;
    }

    @Test
    @DisplayName("reload：DTO 未携带 configKey 时回退到入参 key，不得出现「旧实例已删、新实例未注册」")
    void reloadFallsBackToParameterKeyWhenDtoKeyBlank() {
        IOssService oldService = mockService();
        factory.register("inst", oldService);

        IOssService newService = mockService();
        // 真实形态：调用方知道 key 是 "inst"，但配置对象是新建的、没带 configKey
        OssConfigDTO reloaded = config(null, true, false);

        factory.reload("inst", reloaded, cfg -> newService);

        assertThat(factory.contains("inst")).isTrue();
        assertThat(factory.getService("inst")).isSameAs(newService);
    }

    @Test
    @DisplayName("reload：DTO 自带 configKey 时，移除与注册使用同一个 key")
    void reloadUsesSameKeyForRemoveAndRegister() {
        IOssService newService = mockService();
        OssConfigDTO reloaded = config("inst", true, false);
        factory.register("inst", mockService());

        factory.reload("inst", reloaded, cfg -> newService);

        assertThat(factory.getService("inst")).isSameAs(newService);
    }

    // ========== register / getService ==========

    @Test
    @DisplayName("register：空白 configKey 或 null service 抛 OssException")
    void registerRejectsInvalidArgs() {
        assertThatThrownBy(() -> factory.register("  ", mockService()))
                .isInstanceOf(OssException.class).hasMessage("OSS 配置键不能为空");
        assertThatThrownBy(() -> factory.register("k", null))
                .isInstanceOf(OssException.class).hasMessage("OSS 服务实例不能为空");
    }

    @Test
    @DisplayName("register 后 getService 返回同一实例；contains 为 true")
    void registerThenGet() {
        IOssService service = mockService();

        factory.register("default", service);

        assertThat(factory.getService("default")).isSameAs(service);
        assertThat(factory.contains("default")).isTrue();
    }

    @Test
    @DisplayName("覆盖注册不同实例：关闭旧实例，返回新实例")
    void registerReplacesAndClosesOld() throws Exception {
        IOssService old = mockService();
        IOssService neu = mockService();

        factory.register("k", old);
        factory.register("k", neu);

        assertThat(factory.getService("k")).isSameAs(neu);
        verify(old).close();
    }

    @Test
    @DisplayName("getService：未注册抛 OssException；null/空白 key 抛 OssException（不泄露 NPE）")
    void getServiceFailures() {
        assertThatThrownBy(() -> factory.getService("missing"))
                .isInstanceOf(OssException.class).hasMessageContaining("missing");
        assertThatThrownBy(() -> factory.getService(null))
                .isInstanceOf(OssException.class);
        assertThatThrownBy(() -> factory.getService("  "))
                .isInstanceOf(OssException.class);
    }

    @Test
    @DisplayName("getDefaultService：默认 key 未注册抛 OssException；注册后返回")
    void defaultService() {
        assertThatThrownBy(factory::getDefaultService).isInstanceOf(OssException.class);

        IOssService service = mockService();
        factory.register("default", service);

        assertThat(factory.getDefaultService()).isSameAs(service);
    }

    @Test
    @DisplayName("setDefaultConfigKey 改变默认实例指向")
    void setDefaultConfigKey() {
        IOssService a = mockService();
        IOssService b = mockService();
        factory.register("a", a);
        factory.register("b", b);

        factory.setDefaultConfigKey("b");

        assertThat(factory.getDefaultService()).isSameAs(b);
    }

    // ========== remove ==========

    @Test
    @DisplayName("remove：移除并关闭实例；移除不存在的 key 不抛异常")
    void removeClosesAndToleratesMissing() throws Exception {
        IOssService service = mockService();
        factory.register("k", service);

        factory.remove("k");

        assertThat(factory.contains("k")).isFalse();
        verify(service).close();

        // 再次移除不存在的 key 不应抛异常
        factory.remove("k");
        factory.remove("never-registered");
    }

    @Test
    @DisplayName("remove：close 抛异常被吞掉（仅记录），实例仍被移除")
    void removeSwallowsCloseFailure() throws Exception {
        IOssService service = mockService();
        doThrow(new RuntimeException("close boom")).when(service).close();
        factory.register("k", service);

        factory.remove("k");

        assertThat(factory.contains("k")).isFalse();
    }

    // ========== getAllServices 快照隔离 ==========

    @Test
    @DisplayName("getAllServices 返回副本，修改副本不影响工厂内部状态")
    void getAllServicesReturnsCopy() {
        factory.register("a", mockService());

        Map<String, IOssService> snapshot = factory.getAllServices();
        snapshot.remove("a");

        assertThat(factory.contains("a")).isTrue();
    }

    // ========== registerFromConfig ==========

    @Test
    @DisplayName("registerFromConfig：null 配置抛 OssException")
    void registerFromConfigRejectsNull() {
        assertThatThrownBy(() -> factory.registerFromConfig(null, c -> mockService()))
                .isInstanceOf(OssException.class).hasMessage("OSS 配置不能为空");
    }

    @Test
    @DisplayName("registerFromConfig：disabled 跳过注册")
    void registerFromConfigSkipsDisabled() {
        factory.registerFromConfig(config("k", false, false), c -> mockService());

        assertThat(factory.contains("k")).isFalse();
    }

    @Test
    @DisplayName("registerFromConfig：isDefault=true 时更新默认 key")
    void registerFromConfigSetsDefault() {
        IOssService service = mockService();

        factory.registerFromConfig(config("primary", true, true), c -> service);

        assertThat(factory.getDefaultConfigKey()).isEqualTo("primary");
        assertThat(factory.getDefaultService()).isSameAs(service);
    }

    // ========== registerFromProperties ==========

    private OssInstanceProperties instanceProps(OssTypeEnum type, boolean enabled) {
        OssInstanceProperties p = new OssInstanceProperties();
        p.setOssType(type);
        p.setEnabled(enabled);
        p.setEndpoint("http://localhost:9000");
        p.setBucket("bkt");
        return p;
    }

    @Test
    @DisplayName("registerFromProperties：configs 为空时跳过")
    void registerFromPropertiesEmpty() {
        OssProperties properties = new OssProperties();

        factory.registerFromProperties(properties, OssTypeEnum.MINIO, c -> mockService());

        assertThat(factory.getAllServices()).isEmpty();
    }

    @Test
    @DisplayName("registerFromProperties：仅注册匹配类型且启用的实例")
    void registerFromPropertiesFiltersByTypeAndEnabled() {
        Map<String, OssInstanceProperties> configs = new HashMap<>();
        configs.put("minio1", instanceProps(OssTypeEnum.MINIO, true));
        configs.put("aliyun1", instanceProps(OssTypeEnum.ALIYUN, true));
        configs.put("minioDisabled", instanceProps(OssTypeEnum.MINIO, false));
        OssProperties properties = new OssProperties();
        properties.setConfigs(configs);

        factory.registerFromProperties(properties, OssTypeEnum.MINIO, c -> mockService());

        assertThat(factory.contains("minio1")).isTrue();
        assertThat(factory.contains("aliyun1")).isFalse();
        assertThat(factory.contains("minioDisabled")).isFalse();
    }

    @Test
    @DisplayName("registerFromProperties：单个实例创建失败仅记录日志，其余仍注册")
    void registerFromPropertiesContinuesOnFailure() {
        Map<String, OssInstanceProperties> configs = new HashMap<>();
        configs.put("good", instanceProps(OssTypeEnum.MINIO, true));
        configs.put("bad", instanceProps(OssTypeEnum.MINIO, true));
        OssProperties properties = new OssProperties();
        properties.setConfigs(configs);

        factory.registerFromProperties(properties, OssTypeEnum.MINIO, c -> {
            if ("bad".equals(c.getConfigKey())) {
                throw new OssException("create boom");
            }
            return mockService();
        });

        assertThat(factory.contains("good")).isTrue();
        assertThat(factory.contains("bad")).isFalse();
    }

    // ========== reload ==========

    @Test
    @DisplayName("reload：null 配置抛 OssException；disabled 跳过")
    void reloadGuards() {
        assertThatThrownBy(() -> factory.reload("k", null, c -> mockService()))
                .isInstanceOf(OssException.class).hasMessage("OSS 配置不能为空");

        factory.reload("k", config("k", false, false), c -> mockService());
        assertThat(factory.contains("k")).isFalse();
    }

    @Test
    @DisplayName("reload：先关闭旧实例再注册新实例")
    void reloadReplacesOld() throws Exception {
        IOssService old = mockService();
        IOssService neu = mockService();
        factory.register("k", old);

        factory.reload("k", config("k", true, false), c -> neu);

        assertThat(factory.getService("k")).isSameAs(neu);
        verify(old).close();
    }

    // ========== destroy ==========

    @Test
    @DisplayName("destroy：关闭全部实例；单个关闭失败不中断其余")
    void destroyClosesAll() throws Exception {
        IOssService a = mockService();
        IOssService b = mockService();
        doThrow(new RuntimeException("close boom")).when(a).close();
        factory.register("a", a);
        factory.register("b", b);

        factory.destroy();

        verify(a).close();
        verify(b).close();
    }

    @Test
    @DisplayName("destroy：空工厂不抛异常")
    void destroyEmptyIsNoop() {
        assertThat(factory.getAllServices()).isEmpty();
        factory.destroy();
    }
}
