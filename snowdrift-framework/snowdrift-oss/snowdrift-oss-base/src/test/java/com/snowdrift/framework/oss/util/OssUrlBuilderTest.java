package com.snowdrift.framework.oss.util;

import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OssUrlBuilderTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS URL 构建黑盒测试：域名/虚拟主机/路径三种风格的拼接、首尾斜杠归一、协议保留与缺省 https、空参抛 OssException
 * @since 1.0.0
 */
class OssUrlBuilderTest {

    // ========== buildUrl ==========

    @Test
    @DisplayName("buildUrl：域名 + objectKey 正常拼接")
    void buildUrlNormal() {
        assertThat(OssUrlBuilder.buildUrl("https://cdn.example.com", "images/photo.jpg"))
                .isEqualTo("https://cdn.example.com/images/photo.jpg");
    }

    @Test
    @DisplayName("buildUrl：域名尾斜杠与 key 首斜杠归一，避免双斜杠")
    void buildUrlNormalizesSlashes() {
        assertThat(OssUrlBuilder.buildUrl("http://localhost:8080/files/", "/avatar.png"))
                .isEqualTo("http://localhost:8080/files/avatar.png");
    }

    @Test
    @DisplayName("buildUrl：domain 或 objectKey 空白抛 OssException")
    void buildUrlRejectsBlank() {
        assertThatThrownBy(() -> OssUrlBuilder.buildUrl("  ", "key"))
                .isInstanceOf(OssException.class).hasMessage("OSS 域名不能为空");
        assertThatThrownBy(() -> OssUrlBuilder.buildUrl("https://cdn.example.com", ""))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能为空");
    }

    // ========== buildVirtualHostUrl ==========

    @Test
    @DisplayName("buildVirtualHostUrl：无协议端点默认 https，bucket 作为子域")
    void virtualHostDefaultsHttps() {
        assertThat(OssUrlBuilder.buildVirtualHostUrl("my-bucket", "oss-cn-hangzhou.aliyuncs.com", "images/photo.jpg"))
                .isEqualTo("https://my-bucket.oss-cn-hangzhou.aliyuncs.com/images/photo.jpg");
    }

    @Test
    @DisplayName("buildVirtualHostUrl：端点显式 http:// 时保留 http，并剥离协议后重组")
    void virtualHostKeepsHttp() {
        assertThat(OssUrlBuilder.buildVirtualHostUrl("my-bucket", "http://cos.ap-beijing.myqcloud.com", "avatar.png"))
                .isEqualTo("http://my-bucket.cos.ap-beijing.myqcloud.com/avatar.png");
    }

    @Test
    @DisplayName("buildVirtualHostUrl：端点 https:// 前缀被剥离后仍以 https 重组；尾斜杠/首斜杠归一")
    void virtualHostNormalizes() {
        assertThat(OssUrlBuilder.buildVirtualHostUrl("b", "https://ep.com/", "/k.jpg"))
                .isEqualTo("https://b.ep.com/k.jpg");
    }

    @Test
    @DisplayName("buildVirtualHostUrl：bucket/endpoint/objectKey 任一空白抛 OssException")
    void virtualHostRejectsBlank() {
        assertThatThrownBy(() -> OssUrlBuilder.buildVirtualHostUrl("", "ep.com", "k"))
                .isInstanceOf(OssException.class).hasMessage("OSS 桶名称不能为空");
        assertThatThrownBy(() -> OssUrlBuilder.buildVirtualHostUrl("b", " ", "k"))
                .isInstanceOf(OssException.class).hasMessage("OSS 端点不能为空");
        assertThatThrownBy(() -> OssUrlBuilder.buildVirtualHostUrl("b", "ep.com", null))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能为空");
    }

    // ========== buildPathStyleUrl ==========

    @Test
    @DisplayName("buildPathStyleUrl：endpoint/bucket/objectKey 拼接")
    void pathStyleNormal() {
        assertThat(OssUrlBuilder.buildPathStyleUrl("http://localhost:9000", "my-bucket", "images/photo.jpg"))
                .isEqualTo("http://localhost:9000/my-bucket/images/photo.jpg");
    }

    @Test
    @DisplayName("buildPathStyleUrl：端点尾斜杠与 key 首斜杠归一")
    void pathStyleNormalizesSlashes() {
        assertThat(OssUrlBuilder.buildPathStyleUrl("http://localhost:9000/", "my-bucket", "/k.jpg"))
                .isEqualTo("http://localhost:9000/my-bucket/k.jpg");
    }

    @Test
    @DisplayName("buildPathStyleUrl：endpoint/bucket/objectKey 任一空白抛 OssException")
    void pathStyleRejectsBlank() {
        assertThatThrownBy(() -> OssUrlBuilder.buildPathStyleUrl("", "b", "k"))
                .isInstanceOf(OssException.class).hasMessage("OSS 端点不能为空");
        assertThatThrownBy(() -> OssUrlBuilder.buildPathStyleUrl("http://ep", " ", "k"))
                .isInstanceOf(OssException.class).hasMessage("OSS 桶名称不能为空");
        assertThatThrownBy(() -> OssUrlBuilder.buildPathStyleUrl("http://ep", "b", ""))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能为空");
    }
}
