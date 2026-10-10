package com.snowdrift.framework.oss.properties;

import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OssPropertiesDefaultsTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 配置属性默认值黑盒测试：顶层 defaultConfigKey/configs 与实例级默认（对齐 README 配置参考）
 * @since 1.0.0
 */
class OssPropertiesDefaultsTest {

    @Test
    @DisplayName("OssProperties 默认：defaultConfigKey=default、configs 为空 Map")
    void ossPropertiesDefaults() {
        OssProperties properties = new OssProperties();

        assertThat(properties.getDefaultConfigKey()).isEqualTo("default");
        assertThat(properties.getConfigs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("OssInstanceProperties 默认值符合 README 配置参考")
    void instancePropertiesDefaults() {
        OssInstanceProperties p = new OssInstanceProperties();

        assertThat(p.getEnabled()).isTrue();
        assertThat(p.getIsDefault()).isFalse();
        assertThat(p.getUrlStyle()).isEqualTo(UrlStyleEnum.PATH_STYLE);
        assertThat(p.getPrivateBucket()).isFalse();
        assertThat(p.getSignatureExpiry()).isEqualTo(60);
        assertThat(p.getChunkSize()).isEqualTo(10 * 1024 * 1024L);
        assertThat(p.getUploadTokenExpire()).isEqualTo(30);
        assertThat(p.getChunkUploadUrlExpire()).isEqualTo(120);
        assertThat(p.getOrphanFileCleanupDays()).isEqualTo(7);
        assertThat(p.getMultipartCleanupHours()).isEqualTo(24);
        assertThat(p.getOssType()).isNull();
    }
}
