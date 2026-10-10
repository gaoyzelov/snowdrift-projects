package com.snowdrift.framework.oss.tencent.service;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TencentOssServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 腾讯云 COS 实现黑盒测试（离线可验证部分）：构造期必填项 fail-fast、公开 Bucket URL（domain 路径风格）
 * 与配置访问器。上传/下载/删除/私有签名 URL 依赖真实 COS 服务，属集成测试范畴。
 * @since 1.0.0
 */
class TencentOssServiceImplTest {

    private static final String DOMAIN = "https://cdn.example.com";

    private OssConfigDTO config(String accessKey, String secretKey, String bucket, String domain, String region) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("tencent");
        config.setOssType(OssTypeEnum.TENCENT);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        config.setDomain(domain);
        config.setRegion(region);
        config.setPrivateBucket(false);
        return config;
    }

    @Test
    @DisplayName("构造：accessKey/secretKey/bucket/domain/region 任一空白均 fail-fast 抛 OssException")
    void constructorValidatesRequiredConfig() {
        assertThatThrownBy(() -> new TencentOssServiceImpl(config("", "sk", "bkt", DOMAIN, "ap-beijing")))
                .isInstanceOf(OssException.class).hasMessage("OSS 腾讯云访问密钥不能为空");
        assertThatThrownBy(() -> new TencentOssServiceImpl(config("ak", " ", "bkt", DOMAIN, "ap-beijing")))
                .isInstanceOf(OssException.class).hasMessage("OSS 腾讯云安全密钥不能为空");
        assertThatThrownBy(() -> new TencentOssServiceImpl(config("ak", "sk", "", DOMAIN, "ap-beijing")))
                .isInstanceOf(OssException.class).hasMessage("OSS 腾讯云存储桶不能为空");
        assertThatThrownBy(() -> new TencentOssServiceImpl(config("ak", "sk", "bkt", null, "ap-beijing")))
                .isInstanceOf(OssException.class).hasMessage("OSS 腾讯云域名不能为空");
        assertThatThrownBy(() -> new TencentOssServiceImpl(config("ak", "sk", "bkt", DOMAIN, " ")))
                .isInstanceOf(OssException.class).hasMessage("OSS 腾讯云区域不能为空");
    }

    @Test
    @DisplayName("getUrl：公开 Bucket 且配置 domain 时走路径风格 domain/bucket/key")
    void getUrlPublicWithDomain() {
        TencentOssServiceImpl service =
                new TencentOssServiceImpl(config("ak", "sk", "test-bucket", DOMAIN, "ap-beijing"));
        try {
            assertThat(service.getUrl("a/b.jpg", null))
                    .isEqualTo("https://cdn.example.com/test-bucket/a/b.jpg");
        } finally {
            service.close();
        }
    }

    @Test
    @DisplayName("getType/getBucket/getConfigKey 取自配置")
    void accessors() {
        TencentOssServiceImpl service =
                new TencentOssServiceImpl(config("ak", "sk", "test-bucket", DOMAIN, "ap-beijing"));
        try {
            assertThat(service.getType()).isEqualTo(OssTypeEnum.TENCENT);
            assertThat(service.getBucket()).isEqualTo("test-bucket");
            assertThat(service.getConfigKey()).isEqualTo("tencent");
        } finally {
            service.close();
        }
    }
}
