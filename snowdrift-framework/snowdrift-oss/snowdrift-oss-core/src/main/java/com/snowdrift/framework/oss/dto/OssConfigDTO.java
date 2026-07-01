package com.snowdrift.framework.oss.dto;

import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import lombok.Data;

/**
 * OSS 配置 DTO
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description OSS 存储配置信息，支持静态配置和数据库动态配置
 * @since 1.0.0
 */
@Data
public class OssConfigDTO {
    
    /**
     * 配置标识（如：default、backup）
     */
    private String configKey;
    
    /**
     * 存储类型
     */
    private OssTypeEnum ossType;
    
    // ========== 上传配置 ==========
    
    /**
     * 上传 Endpoint（内网）
     * 示例：http://192.168.1.100:9000
     */
    private String endpoint;
    
    /**
     * Access Key
     */
    private String accessKey;
    
    /**
     * Secret Key
     */
    private String secretKey;
    
    // ========== 访问配置 ==========
    
    /**
     * 访问域名（公网/CDN）
     * 示例：https://oss.example.com
     */
    private String domain;
    
    /**
     * Bucket 名称
     */
    private String bucket;
    
    /**
     * 区域
     * 示例：us-east-1, cn-hangzhou
     */
    private String region;
    
    /**
     * URL 风格
     */
    private UrlStyleEnum urlStyle = UrlStyleEnum.PATH_STYLE;
    
    /**
     * 路径前缀（用于单桶多服务隔离）
     * 示例：service-a/
     */
    private String pathPrefix;
    
    /**
     * 是否为私有 Bucket
     * true: 需要签名 URL 才能访问
     * false: 公开访问
     */
    private Boolean privateBucket = false;
    
    /**
     * 签名 URL 默认有效期（分钟）
     */
    private Integer signatureExpiry = 60;
    
    // ========== 分片上传配置 ==========
    
    /**
     * 分片大小（字节，默认 10MB）
     */
    private Long chunkSize = 10 * 1024 * 1024L;
    
    // ========== 过期时间配置 ==========
    
    /**
     * 上传凭证有效期（分钟，默认 30）
     */
    private Integer uploadTokenExpire = 30;
    
    /**
     * 分片上传 URL 有效期（分钟，默认 2 小时）
     */
    private Integer chunkUploadUrlExpire = 120;
    
    // ========== 清理策略配置 ==========
    
    /**
     * 孤儿文件清理周期（天，默认 7 天）
     */
    private Integer orphanFileCleanupDays = 7;
    
    /**
     * 分片上传清理周期（小时，默认 24 小时）
     */
    private Integer multipartCleanupHours = 24;

    
    // ========== 状态配置 ==========
    
    /**
     * 是否启用
     */
    private Boolean enabled = true;
    
    /**
     * 是否默认配置
     */
    private Boolean isDefault = false;
    
    /**
     * 备注
     */
    private String remark;
}
