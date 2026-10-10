package com.snowdrift.framework.oss.aliyun.service;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AliyunOssServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 阿里云 OSS 实现黑盒测试（离线可验证部分）：构造期必填项 fail-fast 校验、公开 Bucket 的 URL 生成
 * （domain 走路径风格、无 domain 走虚拟主机风格）与配置访问器。上传/下载/删除/私有签名 URL 依赖真实 OSS 服务，
 * 属集成测试范畴，未在单测覆盖。
 * @since 1.0.0
 */
class AliyunOssServiceImplTest {

    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";

    private OssConfigDTO config(String endpoint, String accessKey, String secretKey, String bucket, String domain) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("aliyun");
        config.setOssType(OssTypeEnum.ALIYUN);
        config.setEndpoint(endpoint);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        config.setDomain(domain);
        config.setPrivateBucket(false);
        return config;
    }

    @Test
    @DisplayName("构造：endpoint/accessKey/secretKey/bucket 任一空白均 fail-fast 抛 OssException")
    void constructorValidatesRequiredConfig() {
        assertThatThrownBy(() -> new AliyunOssServiceImpl(config("", "ak", "sk", "bkt", null)))
                .isInstanceOf(OssException.class).hasMessage("OSS 阿里云端点不能为空");
        assertThatThrownBy(() -> new AliyunOssServiceImpl(config(ENDPOINT, " ", "sk", "bkt", null)))
                .isInstanceOf(OssException.class).hasMessage("OSS 阿里云访问密钥不能为空");
        assertThatThrownBy(() -> new AliyunOssServiceImpl(config(ENDPOINT, "ak", "", "bkt", null)))
                .isInstanceOf(OssException.class).hasMessage("OSS 阿里云安全密钥不能为空");
        assertThatThrownBy(() -> new AliyunOssServiceImpl(config(ENDPOINT, "ak", "sk", " ", null)))
                .isInstanceOf(OssException.class).hasMessage("OSS 阿里云存储桶不能为空");
    }

    @Test
    @DisplayName("getUrl：公开 Bucket 且配置 domain 时走路径风格 domain/bucket/key")
    void getUrlPublicWithDomain() {
        AliyunOssServiceImpl service =
                new AliyunOssServiceImpl(config(ENDPOINT, "ak", "sk", "test-bucket", "https://cdn.example.com"));
        try {
            assertThat(service.getUrl("a/b.jpg", null))
                    .isEqualTo("https://cdn.example.com/test-bucket/a/b.jpg");
        } finally {
            service.close();
        }
    }

    @Test
    @DisplayName("getUrl：公开 Bucket 且无 domain 时走虚拟主机风格 bucket.endpoint/key")
    void getUrlPublicWithoutDomain() {
        AliyunOssServiceImpl service =
                new AliyunOssServiceImpl(config(ENDPOINT, "ak", "sk", "test-bucket", null));
        try {
            assertThat(service.getUrl("a/b.jpg", null))
                    .isEqualTo("https://test-bucket.oss-cn-hangzhou.aliyuncs.com/a/b.jpg");
        } finally {
            service.close();
        }
    }

    @Test
    @DisplayName("getType/getBucket/getConfigKey 取自配置")
    void accessors() {
        AliyunOssServiceImpl service =
                new AliyunOssServiceImpl(config(ENDPOINT, "ak", "sk", "test-bucket", "https://cdn.example.com"));
        try {
            assertThat(service.getType()).isEqualTo(OssTypeEnum.ALIYUN);
            assertThat(service.getBucket()).isEqualTo("test-bucket");
            assertThat(service.getConfigKey()).isEqualTo("aliyun");
        } finally {
            service.close();
        }
    }
}
