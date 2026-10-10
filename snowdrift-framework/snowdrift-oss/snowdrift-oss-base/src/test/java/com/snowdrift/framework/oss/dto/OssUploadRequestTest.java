package com.snowdrift.framework.oss.dto;

import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OssUploadRequestTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 上传请求黑盒测试：validate 的空白 key/空流/负大小/合法分支，from(File) 的字段填充与流可读，
 * 以及不存在文件抛 IOException
 * @since 1.0.0
 */
class OssUploadRequestTest {

    private InputStream stream() {
        return new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    }

    // ========== validate ==========

    @Test
    @DisplayName("validate：objectKey 空白抛 OssException")
    void validateRejectsBlankKey() {
        OssUploadRequest request = OssUploadRequest.builder()
                .objectKey("  ").inputStream(stream()).size(4L).build();

        assertThatThrownBy(request::validate)
                .isInstanceOf(OssException.class).hasMessage("OSS 对象键不能为空");
    }

    @Test
    @DisplayName("validate：inputStream 为 null 抛 OssException")
    void validateRejectsNullStream() {
        OssUploadRequest request = OssUploadRequest.builder()
                .objectKey("k").inputStream(null).build();

        assertThatThrownBy(request::validate)
                .isInstanceOf(OssException.class).hasMessage("OSS 上传输入流不能为空");
    }

    @Test
    @DisplayName("validate：size 为负抛 OssException")
    void validateRejectsNegativeSize() {
        OssUploadRequest request = OssUploadRequest.builder()
                .objectKey("k").inputStream(stream()).size(-1L).build();

        assertThatThrownBy(request::validate)
                .isInstanceOf(OssException.class).hasMessage("OSS 上传文件大小不能为负");
    }

    @Test
    @DisplayName("validate：size 为 null 或 0 合法（仅负值被拒）")
    void validateAllowsNullOrZeroSize() {
        OssUploadRequest nullSize = OssUploadRequest.builder()
                .objectKey("k").inputStream(stream()).build();
        OssUploadRequest zeroSize = OssUploadRequest.builder()
                .objectKey("k").inputStream(stream()).size(0L).build();

        nullSize.validate();
        zeroSize.validate();
    }

    @Test
    @DisplayName("validate：完整合法请求不抛异常")
    void validatePasses() {
        OssUploadRequest request = OssUploadRequest.builder()
                .objectKey("dir/k.txt").inputStream(stream()).size(4L).contentType("text/plain")
                .metadata(Map.of("a", "b")).build();

        request.validate();

        assertThat(request.getMetadata()).containsEntry("a", "b");
        assertThat(request.getContentType()).isEqualTo("text/plain");
    }

    // ========== from(File) ==========

    @Test
    @DisplayName("from(File)：objectKey=文件名、size=文件长度、流内容可读")
    void fromFilePopulatesFields(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello", StandardCharsets.UTF_8);

        OssUploadRequest request = OssUploadRequest.from(file.toFile());
        try {
            assertThat(request.getObjectKey()).isEqualTo("hello.txt");
            assertThat(request.getSize()).isEqualTo(5L);
            assertThat(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");
        } finally {
            request.getInputStream().close();
        }
    }

    @Test
    @DisplayName("from(File)：文件不存在时抛 IOException（打开流为最后一步，不泄漏）")
    void fromMissingFileThrows(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.txt");

        assertThatThrownBy(() -> OssUploadRequest.from(missing.toFile()))
                .isInstanceOf(IOException.class);
    }
}
