package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractOssServiceTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 抽象基类黑盒测试（经桩子类）：objectKey 规范化与 UUID 重建、路径穿越拒绝、批量删除容错、
 * 配置访问器与统一异常包装
 * @since 1.0.0
 */
class AbstractOssServiceTest {

    /** 记录 delete 调用、可按 key 抛异常的最小子类 */
    static class StubOssService extends AbstractOssService {
        final List<String> deleted = new ArrayList<>();
        String failOnDelete;

        StubOssService(OssConfigDTO config) {
            super(config);
        }

        String buildKey(String objectKey) {
            return buildObjectKey(objectKey);
        }

        String normalize(String objectKey) {
            return normalizeObjectKey(objectKey);
        }

        OssException error(String msg, String bucket, String key, Exception e) {
            return ossError(msg, bucket, key, e);
        }

        @Override
        public OssResult upload(OssUploadRequest request) {
            return null;
        }

        @Override
        public InputStream download(String objectKey) {
            return null;
        }

        @Override
        public void delete(String objectKey) {
            if (objectKey.equals(failOnDelete)) {
                throw new OssException("delete boom");
            }
            deleted.add(objectKey);
        }

        @Override
        public boolean exists(String objectKey) {
            return false;
        }

        @Override
        public String getUrl(String objectKey, Duration expiry) {
            return null;
        }

        @Override
        public void close() {
            // no-op
        }
    }

    private OssConfigDTO config(String pathPrefix) {
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("default");
        config.setOssType(OssTypeEnum.LOCAL);
        config.setBucket("bkt");
        config.setPathPrefix(pathPrefix);
        return config;
    }

    // ========== normalizeObjectKey ==========

    @Test
    @DisplayName("normalize：反斜杠转正斜杠、移除前导斜杠")
    void normalizeSlashes() {
        StubOssService service = new StubOssService(config(null));

        assertThat(service.normalize("dir\\photo.jpg")).isEqualTo("dir/photo.jpg");
        assertThat(service.normalize("///photo.jpg")).isEqualTo("photo.jpg");
        assertThat(service.normalize("photo.jpg")).isEqualTo("photo.jpg");
    }

    @Test
    @DisplayName("normalize：空白 key 抛 OssException")
    void normalizeRejectsBlank() {
        StubOssService service = new StubOssService(config(null));

        assertThatThrownBy(() -> service.normalize("  "))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能为空");
    }

    @Test
    @DisplayName("安全边界：仅由斜杠组成的 key 归一后为空，必须拒绝 —— 否则 delete(\"/\") 会命中存储根")
    void normalizeRejectsRootOnlyKeys() {
        StubOssService service = new StubOssService(config(null));

        // 归一化会剥掉全部前导斜杠，"/" 于是变成空串；不拦截的话下游会把空串解析为存储根目录，
        // delete("/") 就变成「删除存储根」，exists("/") 则恒为真。
        assertThatThrownBy(() -> service.normalize("/"))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能指向存储根");
        assertThatThrownBy(() -> service.normalize("//"))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能指向存储根");
        assertThatThrownBy(() -> service.normalize("\\\\"))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能指向存储根");
    }

    @Test
    @DisplayName("normalize：路径段为 .. 时拒绝（路径穿越防护），文件名内连点不误伤")
    void normalizeRejectsTraversal() {
        StubOssService service = new StubOssService(config(null));

        assertThatThrownBy(() -> service.normalize("a/../../etc/passwd"))
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键包含路径穿越");
        assertThatThrownBy(() -> service.normalize("../secret"))
                .isInstanceOf(OssException.class);
        // "a..b" 不是独立路径段，不应误判
        assertThat(service.normalize("a..b.txt")).isEqualTo("a..b.txt");
    }

    // ========== buildObjectKey ==========

    @Test
    @DisplayName("buildObjectKey：保留目录、文件名替换为 32 位 UUID、保留扩展名、加前缀")
    void buildKeyWithPrefixAndExtension() {
        StubOssService service = new StubOssService(config("upload/"));

        String key = service.buildKey("dir/photo.jpg");

        assertThat(key).matches("upload/dir/[0-9a-f]{32}\\.jpg");
    }

    @Test
    @DisplayName("buildObjectKey：前缀无尾斜杠时自动补斜杠")
    void buildKeyNormalizesPrefixSlash() {
        StubOssService service = new StubOssService(config("upload"));

        assertThat(service.buildKey("photo.jpg")).matches("upload/[0-9a-f]{32}\\.jpg");
    }

    @Test
    @DisplayName("buildObjectKey：无前缀时直接返回 UUID 文件名")
    void buildKeyWithoutPrefix() {
        StubOssService service = new StubOssService(config(null));

        assertThat(service.buildKey("photo.png")).matches("[0-9a-f]{32}\\.png");
    }

    @Test
    @DisplayName("buildObjectKey：无扩展名时不追加多余点号")
    void buildKeyWithoutExtension() {
        StubOssService service = new StubOssService(config(null));

        assertThat(service.buildKey("README")).matches("[0-9a-f]{32}");
    }

    @Test
    @DisplayName("buildObjectKey：反斜杠路径归一后再重建")
    void buildKeyNormalizesBackslash() {
        StubOssService service = new StubOssService(config("p/"));

        assertThat(service.buildKey("dir\\photo.jpg")).matches("p/dir/[0-9a-f]{32}\\.jpg");
    }

    @Test
    @DisplayName("buildObjectKey：路径穿越 key 抛 OssException")
    void buildKeyRejectsTraversal() {
        StubOssService service = new StubOssService(config(null));

        assertThatThrownBy(() -> service.buildKey("../../etc/passwd"))
                .isInstanceOf(OssException.class);
    }

    // ========== deleteBatch ==========

    @Test
    @DisplayName("deleteBatch：null / 空列表不调用 delete")
    void deleteBatchEmpty() {
        StubOssService service = new StubOssService(config(null));

        service.deleteBatch(null);
        service.deleteBatch(List.of());

        assertThat(service.deleted).isEmpty();
    }

    @Test
    @DisplayName("deleteBatch：逐个删除")
    void deleteBatchAll() {
        StubOssService service = new StubOssService(config(null));

        service.deleteBatch(List.of("a", "b", "c"));

        assertThat(service.deleted).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("deleteBatch：单个失败仅记录日志，不中断其余删除")
    void deleteBatchContinuesOnFailure() {
        StubOssService service = new StubOssService(config(null));
        service.failOnDelete = "b";

        service.deleteBatch(List.of("a", "b", "c"));

        assertThat(service.deleted).containsExactly("a", "c");
    }

    // ========== 配置访问器 & 异常包装 ==========

    @Test
    @DisplayName("getBucket/getConfigKey/getType 取自配置")
    void configAccessors() {
        StubOssService service = new StubOssService(config(null));

        assertThat(service.getBucket()).isEqualTo("bkt");
        assertThat(service.getConfigKey()).isEqualTo("default");
        assertThat(service.getType()).isEqualTo(OssTypeEnum.LOCAL);
    }

    @Test
    @DisplayName("ossError：包装为 OssException 并保留 cause")
    void ossErrorWrapsCause() {
        StubOssService service = new StubOssService(config(null));
        RuntimeException cause = new RuntimeException("io");

        OssException ex = service.error("上传失败", "bkt", "k", cause);

        assertThat(ex).isInstanceOf(OssException.class);
        assertThat(ex.getMessage()).isEqualTo("上传失败");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
