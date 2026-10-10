package com.snowdrift.framework.oss.util;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OssConfigConverterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 配置转换黑盒测试：Properties↔DTO 全字段映射、configKey 仅存于 DTO、null 入参抛 OssException、往返一致
 * @since 1.0.0
 */
class OssConfigConverterTest {

    private OssInstanceProperties fullProperties() {
        OssInstanceProperties p = new OssInstanceProperties();
        p.setOssType(OssTypeEnum.MINIO);
        p.setEndpoint("http://localhost:9000");
        p.setAccessKey("ak");
        p.setSecretKey("sk");
        p.setDomain("https://cdn.example.com");
        p.setBucket("bkt");
        p.setRegion("cn-hangzhou");
        p.setUrlStyle(UrlStyleEnum.VIRTUAL_HOST);
        p.setPathPrefix("upload/");
        p.setPrivateBucket(true);
        p.setSignatureExpiry(30);
        p.setChunkSize(2048L);
        p.setUploadTokenExpire(15);
        p.setChunkUploadUrlExpire(90);
        p.setOrphanFileCleanupDays(3);
        p.setMultipartCleanupHours(12);
        p.setEnabled(false);
        p.setIsDefault(true);
        p.setRemark("备注");
        return p;
    }

    @Test
    @DisplayName("fromProperties：全字段映射，configKey 由入参写入 DTO")
    void fromPropertiesMapsAllFields() {
        OssConfigDTO dto = OssConfigConverter.fromProperties(fullProperties(), "myKey");

        assertThat(dto.getConfigKey()).isEqualTo("myKey");
        assertThat(dto.getOssType()).isEqualTo(OssTypeEnum.MINIO);
        assertThat(dto.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(dto.getAccessKey()).isEqualTo("ak");
        assertThat(dto.getSecretKey()).isEqualTo("sk");
        assertThat(dto.getDomain()).isEqualTo("https://cdn.example.com");
        assertThat(dto.getBucket()).isEqualTo("bkt");
        assertThat(dto.getRegion()).isEqualTo("cn-hangzhou");
        assertThat(dto.getUrlStyle()).isEqualTo(UrlStyleEnum.VIRTUAL_HOST);
        assertThat(dto.getPathPrefix()).isEqualTo("upload/");
        assertThat(dto.getPrivateBucket()).isTrue();
        assertThat(dto.getSignatureExpiry()).isEqualTo(30);
        assertThat(dto.getChunkSize()).isEqualTo(2048L);
        assertThat(dto.getUploadTokenExpire()).isEqualTo(15);
        assertThat(dto.getChunkUploadUrlExpire()).isEqualTo(90);
        assertThat(dto.getOrphanFileCleanupDays()).isEqualTo(3);
        assertThat(dto.getMultipartCleanupHours()).isEqualTo(12);
        assertThat(dto.getEnabled()).isFalse();
        assertThat(dto.getIsDefault()).isTrue();
        assertThat(dto.getRemark()).isEqualTo("备注");
    }

    @Test
    @DisplayName("fromProperties(null) 抛 OssException")
    void fromPropertiesRejectsNull() {
        assertThatThrownBy(() -> OssConfigConverter.fromProperties(null, "k"))
                .isInstanceOf(OssException.class).hasMessage("OSS 配置信息不能为空");
    }

    @Test
    @DisplayName("toProperties：全字段映射（Properties 无 configKey 字段，故不回写）")
    void toPropertiesMapsAllFields() {
        OssConfigDTO dto = OssConfigConverter.fromProperties(fullProperties(), "myKey");

        OssInstanceProperties p = OssConfigConverter.toProperties(dto);

        assertThat(p.getOssType()).isEqualTo(OssTypeEnum.MINIO);
        assertThat(p.getEndpoint()).isEqualTo("http://localhost:9000");
        assertThat(p.getBucket()).isEqualTo("bkt");
        assertThat(p.getUrlStyle()).isEqualTo(UrlStyleEnum.VIRTUAL_HOST);
        assertThat(p.getPrivateBucket()).isTrue();
        assertThat(p.getChunkSize()).isEqualTo(2048L);
        assertThat(p.getEnabled()).isFalse();
        assertThat(p.getIsDefault()).isTrue();
        assertThat(p.getRemark()).isEqualTo("备注");
    }

    @Test
    @DisplayName("toProperties(null) 抛 OssException")
    void toPropertiesRejectsNull() {
        assertThatThrownBy(() -> OssConfigConverter.toProperties(null))
                .isInstanceOf(OssException.class).hasMessage("OSS 配置信息不能为空");
    }

    @Test
    @DisplayName("往返转换（Properties→DTO→Properties）关键字段保持一致")
    void roundTripPreservesFields() {
        OssInstanceProperties origin = fullProperties();

        OssInstanceProperties back = OssConfigConverter.toProperties(
                OssConfigConverter.fromProperties(origin, "k"));

        assertThat(back.getOssType()).isEqualTo(origin.getOssType());
        assertThat(back.getEndpoint()).isEqualTo(origin.getEndpoint());
        assertThat(back.getBucket()).isEqualTo(origin.getBucket());
        assertThat(back.getSignatureExpiry()).isEqualTo(origin.getSignatureExpiry());
        assertThat(back.getChunkSize()).isEqualTo(origin.getChunkSize());
    }
}
