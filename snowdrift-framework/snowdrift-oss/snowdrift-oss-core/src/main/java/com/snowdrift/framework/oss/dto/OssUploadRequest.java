package com.snowdrift.framework.oss.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.util.Map;

/**
 * OSS 上传请求
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
}
