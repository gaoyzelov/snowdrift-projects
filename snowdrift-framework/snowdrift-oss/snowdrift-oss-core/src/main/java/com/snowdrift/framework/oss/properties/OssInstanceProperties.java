package com.snowdrift.framework.oss.properties;

import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * OssInstanceProperties
 *
 * @author 83674
 * @date 2026/5/9-13:07
 * @description OSS实例配置
 * @since 1.0.0
 */
@Data
public class OssInstanceProperties implements Serializable {

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

    /**
     * 存储类型
     */
    private OssTypeEnum ossType;

    /**
     * 上传 Endpoint（内网）
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

    /**
     * 访问域名（公网/CDN）
     */
    private String domain;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * 区域
     */
    private String region;

    /**
     * URL 风格
     */
    private UrlStyleEnum urlStyle = UrlStyleEnum.PATH_STYLE;

    /**
     * 路径前缀
     */
    private String pathPrefix;

    /**
     * 是否为私有 Bucket
     */
    private Boolean privateBucket = false;

    /**
     * 签名 URL 默认有效期（分钟）
     */
    private Integer signatureExpiry = 60;

    /**
     * 分片大小（字节）
     */
    private Long chunkSize = 5 * 1024 * 1024L;

    /**
     * 上传凭证有效期（分钟）
     */
    private Integer uploadTokenExpire = 30;

    /**
     * 分片上传 URL 有效期（分钟）
     */
    private Integer chunkUploadUrlExpire = 120;

    /**
     * 孤儿文件清理周期（天）
     */
    private Integer orphanFileCleanupDays = 7;

    /**
     * 分片上传清理周期（小时）
     */
    private Integer multipartCleanupHours = 24;
}
