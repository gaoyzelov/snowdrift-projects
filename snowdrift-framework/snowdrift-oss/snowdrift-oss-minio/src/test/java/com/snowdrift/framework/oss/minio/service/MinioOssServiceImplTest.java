package com.snowdrift.framework.oss.minio.service;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MinioOssServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description MinIO 实现黑盒测试（离线可验证部分）：构造期必填项 fail-fast 校验（在建立客户端与联网检查桶之前）。
 * 说明：MinIO 构造成功路径会调用 ensureBucketExists 做真实联网桶检查，故成功构造与上传/下载/删除/URL 等
 * 依赖真实 MinIO 服务，属集成测试范畴，未在单测覆盖。
 * @since 1.0.0
 */
class MinioOssServiceImplTest {

    private OssConfigDTO config(String endpoint, String accessKey, String secretKey, String bucket) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("minio");
        config.setOssType(OssTypeEnum.MINIO);
        config.setEndpoint(endpoint);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        return config;
    }

    @Test
    @DisplayName("构造：endpoint 空白 fail-fast 抛 OssException（先于建客户端/联网）")
    void rejectsBlankEndpoint() {
        assertThatThrownBy(() -> new MinioOssServiceImpl(config("", "ak", "sk", "bkt")))
                .isInstanceOf(OssException.class).hasMessage("OSS MINIO 端点不能为空");
    }

    @Test
    @DisplayName("构造：accessKey 空白 fail-fast 抛 OssException")
    void rejectsBlankAccessKey() {
        assertThatThrownBy(() -> new MinioOssServiceImpl(config("http://localhost:9000", " ", "sk", "bkt")))
                .isInstanceOf(OssException.class).hasMessage("OSS MINIO 访问密钥不能为空");
    }

    @Test
    @DisplayName("构造：secretKey 空白 fail-fast 抛 OssException")
    void rejectsBlankSecretKey() {
        assertThatThrownBy(() -> new MinioOssServiceImpl(config("http://localhost:9000", "ak", "", "bkt")))
                .isInstanceOf(OssException.class).hasMessage("OSS MINIO 安全密钥不能为空");
    }

    @Test
    @DisplayName("构造：bucket 空白 fail-fast 抛 OssException")
    void rejectsBlankBucket() {
        assertThatThrownBy(() -> new MinioOssServiceImpl(config("http://localhost:9000", "ak", "sk", null)))
                .isInstanceOf(OssException.class).hasMessage("OSS MINIO 桶名称不能为空");
    }
}
