package com.snowdrift.framework.oss.dto;

import com.snowdrift.framework.oss.exception.OssException;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.util.Map;

/**
 * OSS 上传请求（充血模型）
 * <p>
 * 封装文件上传所需的参数，并提供统一的校验方法
 * 所有实现类无需重复校验，直接调用 validate() 即可
 *
 * @author 83674
 * @date 2026/5/9
 * @description 文件上传请求参数
 * @since 1.0.0
 */
@Data
@Builder
public class OssUploadRequest {
    
    /**
     * OSS 对象键（文件路径）
     * 示例：images/2024/01/photo.jpg
     */
    private String objectKey;
    
    /**
     * 文件输入流
     */
    private InputStream inputStream;
    
    /**
     * 文件大小（字节）
     */
    private Long size;
    
    /**
     * 文件类型（MIME Type）
     * 示例：image/jpeg, application/pdf
     */
    private String contentType;
    
    /**
     * 元数据（可选）
     */
    private Map<String, String> metadata;
    
    /**
     * 校验上传请求参数的合法性
     * <p>
     * 统一校验所有必填参数和边界值
     * 所有 OSS 实现类应在 upload() 方法开头调用此方法
     *
     * @throws OssException 当参数不合法时抛出
     */
    public void validate() {
        if (this.inputStream == null) {
            throw new OssException("oss.upload.inputstream.null");
        }
        if (this.objectKey == null || this.objectKey.trim().isEmpty()) {
            throw new OssException("oss.object.key.empty");
        }
        if (this.size != null && this.size < 0) {
            throw new OssException("oss.upload.size.invalid", new Object[]{this.size});
        }
    }
}
