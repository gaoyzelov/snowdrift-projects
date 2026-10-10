package com.snowdrift.framework.oss.local.service;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalOssServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 本地存储实现黑盒测试（真实临时目录）：构造校验与建目录、上传落盘与结果、下载/删除/存在性、
 * URL 生成、缺域名前置校验、路径穿越与符号链接逃逸防护、close 幂等
 * @since 1.0.0
 */
class LocalOssServiceImplTest {

    private static final String DOMAIN = "http://files.local";

    private OssConfigDTO config(String endpoint, String domain) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("local");
        config.setOssType(OssTypeEnum.LOCAL);
        config.setEndpoint(endpoint);
        config.setDomain(domain);
        config.setBucket("local-bkt");
        return config;
    }

    private OssUploadRequest uploadRequest(String objectKey, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return OssUploadRequest.builder()
                .objectKey(objectKey)
                .inputStream(new ByteArrayInputStream(bytes))
                .size((long) bytes.length)
                .contentType("text/plain")
                .build();
    }

    // ========== 构造 ==========

    @Test
    @DisplayName("构造：endpoint 空白抛 OssException")
    void rejectsBlankEndpoint(@TempDir Path tempDir) {
        assertThatThrownBy(() -> new LocalOssServiceImpl(config("  ", DOMAIN)))
                .isInstanceOf(OssException.class).hasMessage("OSS 本地端点不能为空");
    }

    @Test
    @DisplayName("构造：存储根目录不存在时自动创建")
    void createsStorageRootIfAbsent(@TempDir Path tempDir) {
        Path root = tempDir.resolve("nested/storage");

        new LocalOssServiceImpl(config(root.toString(), DOMAIN));

        assertThat(Files.isDirectory(root)).isTrue();
    }

    // ========== upload ==========

    @Test
    @DisplayName("上传：文件落盘（UUID 重命名），返回 objectKey/url/size 且内容一致")
    void uploadWritesFileAndReturnsResult(@TempDir Path tempDir) throws IOException {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));

        OssResult result = service.upload(uploadRequest("avatar/pic.txt", "hello"));

        assertThat(result.getObjectKey()).matches("avatar/[0-9a-f]{32}\\.txt");
        assertThat(result.getSize()).isEqualTo(5L);
        assertThat(result.getUrl()).isEqualTo(DOMAIN + "/" + result.getObjectKey());
        Path written = tempDir.resolve(result.getObjectKey());
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readString(written, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    @DisplayName("上传：应用 path-prefix 前缀")
    void uploadAppliesPathPrefix(@TempDir Path tempDir) {
        OssConfigDTO config = config(tempDir.toString(), DOMAIN);
        config.setPathPrefix("upload");
        LocalOssServiceImpl service = new LocalOssServiceImpl(config);

        OssResult result = service.upload(uploadRequest("pic.txt", "x"));

        assertThat(result.getObjectKey()).matches("upload/[0-9a-f]{32}\\.txt");
    }

    @Test
    @DisplayName("上传：缺 domain 时在写盘前抛异常（避免孤儿文件）")
    void uploadRejectsMissingDomain(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), null));

        assertThatThrownBy(() -> service.upload(uploadRequest("pic.txt", "x")))
                .isInstanceOf(OssException.class).hasMessageContaining("缺少域名配置");
    }

    @Test
    @DisplayName("上传：非法请求（空 objectKey）经 validate 抛 OssException")
    void uploadValidatesRequest(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));
        OssUploadRequest bad = OssUploadRequest.builder().objectKey("").inputStream(null).build();

        assertThatThrownBy(() -> service.upload(bad)).isInstanceOf(OssException.class);
    }

    // ========== download / exists / delete ==========

    @Test
    @DisplayName("下载：返回文件内容流；调用方关闭")
    void downloadReturnsContent(@TempDir Path tempDir) throws IOException {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));
        OssResult uploaded = service.upload(uploadRequest("doc.txt", "content"));

        try (InputStream in = service.download(uploaded.getObjectKey())) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content");
        }
    }

    @Test
    @DisplayName("下载：文件不存在抛 OssException")
    void downloadMissingThrows(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));

        assertThatThrownBy(() -> service.download("missing.txt"))
                .isInstanceOf(OssException.class).hasMessage("OSS 本地文件未找到");
    }

    @Test
    @DisplayName("exists：存在 true / 不存在 false")
    void existsReflectsFileSystem(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));
        OssResult uploaded = service.upload(uploadRequest("e.txt", "x"));

        assertThat(service.exists(uploaded.getObjectKey())).isTrue();
        assertThat(service.exists("nope.txt")).isFalse();
    }

    @Test
    @DisplayName("delete：删除已存在文件；删除不存在文件幂等不抛异常")
    void deleteIsIdempotent(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));
        OssResult uploaded = service.upload(uploadRequest("d.txt", "x"));

        service.delete(uploaded.getObjectKey());
        assertThat(service.exists(uploaded.getObjectKey())).isFalse();

        // 再次删除不存在文件不应抛异常
        service.delete(uploaded.getObjectKey());
        service.delete("never-existed.txt");
    }

    @Test
    @DisplayName("deleteBatch：逐条删除（继承自 AbstractOssService）")
    void deleteBatchRemovesAll(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));
        OssResult a = service.upload(uploadRequest("a.txt", "1"));
        OssResult b = service.upload(uploadRequest("b.txt", "2"));

        service.deleteBatch(java.util.List.of(a.getObjectKey(), b.getObjectKey()));

        assertThat(service.exists(a.getObjectKey())).isFalse();
        assertThat(service.exists(b.getObjectKey())).isFalse();
    }

    // ========== getUrl ==========

    @Test
    @DisplayName("getUrl：domain + objectKey；前导斜杠归一避免双斜杠")
    void getUrlBuildsFromDomain(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));

        assertThat(service.getUrl("avatar/pic.jpg", null)).isEqualTo(DOMAIN + "/avatar/pic.jpg");
        assertThat(service.getUrl("/avatar/pic.jpg", null)).isEqualTo(DOMAIN + "/avatar/pic.jpg");
    }

    @Test
    @DisplayName("getUrl：缺 domain 抛 OssException")
    void getUrlRejectsMissingDomain(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), " "));

        assertThatThrownBy(() -> service.getUrl("pic.jpg", null))
                .isInstanceOf(OssException.class).hasMessageContaining("缺少域名配置");
    }

    // ========== 安全防护 ==========

    @Test
    @DisplayName("路径穿越（..）在 download/delete/exists/getUrl 均被拒")
    void rejectsPathTraversal(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));

        assertThatThrownBy(() -> service.download("../../etc/passwd")).isInstanceOf(OssException.class);
        assertThatThrownBy(() -> service.delete("../secret")).isInstanceOf(OssException.class);
        assertThatThrownBy(() -> service.exists("a/../../b")).isInstanceOf(OssException.class);
        assertThatThrownBy(() -> service.getUrl("../../etc/passwd", null)).isInstanceOf(OssException.class);
    }

    @Test
    @DisplayName("符号链接逃逸：存储根内指向外部的软链被拒（环境不支持则跳过）")
    void rejectsSymlinkEscape(@TempDir Path tempDir) throws IOException {
        Path root = tempDir.resolve("root");
        Files.createDirectories(root);
        Path outside = tempDir.resolve("outside");
        Files.createDirectories(outside);
        Path link = root.resolve("link");

        boolean created;
        try {
            Files.createSymbolicLink(link, outside);
            created = true;
        } catch (IOException | UnsupportedOperationException e) {
            created = false;
        }
        Assumptions.assumeTrue(created, "当前环境不支持创建符号链接，跳过");

        LocalOssServiceImpl service = new LocalOssServiceImpl(config(root.toString(), DOMAIN));

        assertThatThrownBy(() -> service.exists("link/any.txt"))
                .isInstanceOf(OssException.class)
                .hasMessageContaining("符号链接");
    }

    // ========== close & 类型 ==========

    @Test
    @DisplayName("close 幂等无副作用；getType/getBucket/getConfigKey 取自配置")
    void closeAndAccessors(@TempDir Path tempDir) {
        LocalOssServiceImpl service = new LocalOssServiceImpl(config(tempDir.toString(), DOMAIN));

        service.close();
        service.close();

        assertThat(service.getType()).isEqualTo(OssTypeEnum.LOCAL);
        assertThat(service.getBucket()).isEqualTo("local-bkt");
        assertThat(service.getConfigKey()).isEqualTo("local");
    }
}
