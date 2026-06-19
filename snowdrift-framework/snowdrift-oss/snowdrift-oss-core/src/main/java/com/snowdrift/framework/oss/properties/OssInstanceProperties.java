package com.snowdrift.framework.oss.properties;

import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.enums.UrlStyleEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * OSS 实例配置属性
 * <p>
 * 用于 YAML/Properties 配置文件中的 OSS 实例配置
 * 支持 Jakarta Validation 校验，启动时自动验证配置合法性
 *
 * @author 83674
 * @date 2026/5/9
 * @description OSS 实例配置
 * @since 1.0.0
 */
@Data
public class OssInstanceProperties implements Serializable {

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled = true;

    /**
     * 是否默认配置
     */
    @NotNull(message = "默认配置状态不能为空")
    private Boolean isDefault = false;

    /**
     * 备注
     */
    private String remark;

    /**
     * 存储类型
     */
    @NotNull(message = "存储类型不能为空")
    private OssTypeEnum ossType;

    /**
     * 上传 Endpoint
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
    @NotNull(message = "URL 风格不能为空")
    private UrlStyleEnum urlStyle = UrlStyleEnum.PATH_STYLE;

    /**
     * 路径前缀
     */
    private String pathPrefix;

    /**
     * 是否为私有 Bucket
     */
    @NotNull(message = "私有 Bucket 状态不能为空")
    private Boolean privateBucket = false;

    /**
     * 签名 URL 默认有效期（分钟）
     */
    @Positive(message = "签名 URL 有效期必须大于 0")
    private Integer signatureExpiry = 60;

    /**
     * 分片大小（字节）
     */
    @Min(value = 1024, message = "分片大小至少为 1024 字节")
    private Long chunkSize = 10 * 1024 * 1024L;

    /**
     * 上传凭证有效期（分钟）
     */
    @Positive(message = "上传凭证有效期必须大于 0")
    private Integer uploadTokenExpire = 30;

    /**
     * 分片上传 URL 有效期（分钟）
     */
    @Positive(message = "分片上传 URL 有效期必须大于 0")
    private Integer chunkUploadUrlExpire = 120;

    /**
     * 孤儿文件清理周期（天）
     */
    @Min(value = 1, message = "孤儿文件清理周期至少为 1 天")
    @Max(value = 365, message = "孤儿文件清理周期最多为 365 天")
    private Integer orphanFileCleanupDays = 7;

    /**
     * 分片上传清理周期（小时）
     */
    @Min(value = 1, message = "分片上传清理周期至少为 1 小时")
    @Max(value = 168, message = "分片上传清理周期最多为 168 小时")
    private Integer multipartCleanupHours = 24;
}
