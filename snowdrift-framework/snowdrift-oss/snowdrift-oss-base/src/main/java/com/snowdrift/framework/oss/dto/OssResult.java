package com.snowdrift.framework.oss.dto;

import lombok.Builder;
import lombok.Data;

/**
 * OSS 上传结果
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 文件上传成功后的返回结果
 * @since 1.0.0
 */
@Data
@Builder
public class OssResult {
    
    /**
     * OSS 对象键
     */
    private String objectKey;
    
    /**
     * 文件访问 URL
     */
    private String url;
    
    /**
     * Bucket 名称
     */
    private String bucket;
    
    /**
     * 文件大小（字节）
     */
    private Long size;
    
    /**
     * 文件 ETag
     */
    private String etag;
    
    /**
     * 存储类型
     */
    private String storageClass;
}
