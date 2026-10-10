package com.snowdrift.framework.oss.qiniu.service;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QiniuOssServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 七牛云实现黑盒测试（离线可验证部分）：构造期必填项 fail-fast、公开空间 URL（domain/key）、
 * 私有空间签名 URL（本地 HMAC 签名，含 token 参数）。上传/下载/删除依赖真实七牛服务，属集成测试范畴。
 * @since 1.0.0
 */
class QiniuOssServiceImplTest {

    private static final String DOMAIN = "https://cdn.example.com";

    private OssConfigDTO config(String accessKey, String secretKey, String bucket, String domain, boolean privateBucket) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("qiniu");
        config.setOssType(OssTypeEnum.QINIU);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucket(bucket);
        config.setDomain(domain);
        config.setPrivateBucket(privateBucket);
        return config;
    }

    @Test
    @DisplayName("构造：accessKey/secretKey/bucket/domain 任一空白均 fail-fast 抛 OssException")
    void constructorValidatesRequiredConfig() {
        assertThatThrownBy(() -> new QiniuOssServiceImpl(config("", "sk", "bkt", DOMAIN, false)))
                .isInstanceOf(OssException.class).hasMessage("OSS 七牛云访问密钥不能为空");
        assertThatThrownBy(() -> new QiniuOssServiceImpl(config("ak", " ", "bkt", DOMAIN, false)))
                .isInstanceOf(OssException.class).hasMessage("OSS 七牛云安全密钥不能为空");
        assertThatThrownBy(() -> new QiniuOssServiceImpl(config("ak", "sk", "", DOMAIN, false)))
                .isInstanceOf(OssException.class).hasMessage("OSS 七牛云存储桶不能为空");
        assertThatThrownBy(() -> new QiniuOssServiceImpl(config("ak", "sk", "bkt", null, false)))
                .isInstanceOf(OssException.class).hasMessage("OSS 七牛云域名不能为空");
    }

    @Test
    @DisplayName("getUrl：公开空间返回 domain/key")
    void getUrlPublic() {
        QiniuOssServiceImpl service = new QiniuOssServiceImpl(config("ak", "sk", "bkt", DOMAIN, false));

        assertThat(service.getUrl("a/b.jpg", null)).isEqualTo("https://cdn.example.com/a/b.jpg");
    }

    @Test
    @DisplayName("getUrl：私有空间返回带签名 token 的 URL（本地签名，不触网）")
    void getUrlPrivateSigned() {
        QiniuOssServiceImpl service = new QiniuOssServiceImpl(config("ak", "sk", "bkt", DOMAIN, true));

        String signedUrl = service.getUrl("a/b.jpg", null);

        assertThat(signedUrl).startsWith("https://cdn.example.com/a/b.jpg");
        assertThat(signedUrl).contains("token=");
    }

    @Test
    @DisplayName("getType/getBucket/getConfigKey 取自配置")
    void accessors() {
        QiniuOssServiceImpl service = new QiniuOssServiceImpl(config("ak", "sk", "test-bucket", DOMAIN, false));

        assertThat(service.getType()).isEqualTo(OssTypeEnum.QINIU);
        assertThat(service.getBucket()).isEqualTo("test-bucket");
        assertThat(service.getConfigKey()).isEqualTo("qiniu");
    }
}
