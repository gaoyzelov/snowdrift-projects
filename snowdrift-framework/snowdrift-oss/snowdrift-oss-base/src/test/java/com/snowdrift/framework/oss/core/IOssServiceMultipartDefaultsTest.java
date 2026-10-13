package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IOssServiceMultipartDefaultsTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description IOssService 分片上传能力的默认契约：未支持分片的后端必须「声明不支持 + 调用即抛」，
 * 第三方实现只实现抽象方法时行为可预测
 * @since 1.0.0
 */
class IOssServiceMultipartDefaultsTest {

    /** 最小实现：只实现抽象方法，全部沿用接口的 default 分片实现 */
    private final IOssService minimal = new IOssService() {

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
            // no-op
        }

        @Override
        public void deleteBatch(List<String> objectKeys) {
            // no-op
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
        public OssTypeEnum getType() {
            return OssTypeEnum.LOCAL;
        }

        @Override
        public String getBucket() {
            return "b";
        }

        @Override
        public String getConfigKey() {
            return "k";
        }

        @Override
        public void close() {
            // 无资源可释放
        }
    };

    @Test
    @DisplayName("默认声明不支持分片上传（无需各后端重复实现）")
    void multipartUnsupportedByDefault() {
        assertThat(minimal.supportsMultipartUpload()).isFalse();
    }

    @Test
    @DisplayName("未实现分片的后端：初始化分片抛 OssException")
    void initiateMultipartUploadThrows() {
        assertThatThrownBy(() -> minimal.initiateMultipartUpload("a.txt", "text/plain"))
                .isInstanceOf(OssException.class)
                .hasMessage("当前存储类型不支持分片上传");
    }

    @Test
    @DisplayName("未实现分片的后端：分片预签名 URL 抛 OssException")
    void presignedChunkUrlThrows() {
        assertThatThrownBy(() -> minimal.generatePresignedUploadUrlForChunk(
                "a.txt", "upload-1", 1, Duration.ofMinutes(10)))
                .isInstanceOf(OssException.class)
                .hasMessage("当前存储类型不支持分片上传");
    }

    @Test
    @DisplayName("未实现分片的后端：合并分片抛 OssException")
    void completeMultipartUploadThrows() {
        assertThatThrownBy(() -> minimal.completeMultipartUpload("a.txt", "upload-1", List.of()))
                .isInstanceOf(OssException.class)
                .hasMessage("当前存储类型不支持分片上传");
    }

    @Test
    @DisplayName("未实现分片的后端：取消分片抛 OssException")
    void abortMultipartUploadThrows() {
        assertThatThrownBy(() -> minimal.abortMultipartUpload("a.txt", "upload-1"))
                .isInstanceOf(OssException.class)
                .hasMessage("当前存储类型不支持分片上传");
    }
}
