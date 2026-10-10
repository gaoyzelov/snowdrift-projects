package com.snowdrift.framework.oss.dto;

import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OssConfigDTOTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 配置 DTO 默认值黑盒测试：锁定 urlStyle/privateBucket/signatureExpiry/chunkSize 等默认契约
 * @since 1.0.0
 */
class OssConfigDTOTest {

    @Test
    @DisplayName("默认值：PATH_STYLE、非私有、签名 60 分、分片 10MB、启用、非默认")
    void defaults() {
        OssConfigDTO dto = new OssConfigDTO();

        assertThat(dto.getUrlStyle()).isEqualTo(UrlStyleEnum.PATH_STYLE);
        assertThat(dto.getPrivateBucket()).isFalse();
        assertThat(dto.getSignatureExpiry()).isEqualTo(60);
        assertThat(dto.getChunkSize()).isEqualTo(10 * 1024 * 1024L);
        assertThat(dto.getUploadTokenExpire()).isEqualTo(30);
        assertThat(dto.getChunkUploadUrlExpire()).isEqualTo(120);
        assertThat(dto.getOrphanFileCleanupDays()).isEqualTo(7);
        assertThat(dto.getMultipartCleanupHours()).isEqualTo(24);
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getIsDefault()).isFalse();
        assertThat(dto.getConfigKey()).isNull();
        assertThat(dto.getOssType()).isNull();
    }
}
