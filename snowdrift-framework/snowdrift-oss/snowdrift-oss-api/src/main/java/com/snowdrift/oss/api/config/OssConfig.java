package com.snowdrift.oss.api.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * OssConfig
 *
 * @author gaoye
 * @date 2025/03/25 15:19:57
 * @description OSS配置
 * @since 1.0.0
 */
@Data
@Builder
@Valid
@Schema(title = "OssConfig", description = "OSS配置")
public class OssConfig implements Serializable {

    @Schema(title = "OSS服务端点", example = "oss-cn-beijing.aliyuncs.com")
    @NotBlank(message = "endpoint不能为空")
    private String endpoint;

    @Schema(title = "访问秘钥", example = "4a96sd7f89s7df9as7df")
    @NotBlank(message = "accessKey不能为空")
    private String accessKey;

    @Schema(title = "安全秘钥", example = "4a96sd7f89s7df9as7df")
    @NotBlank(message = "secretKey不能为空")
    private String secretKey;

    @Schema(title = "存储桶", example = "test")
    private String bucket;

    @Schema(title = "区域", example = "cn-beijing")
    private String region;

    @Schema(title = "域名", example = "https://test.oss-cn-beijing.aliyuncs.com")
    private String domain;

    @Schema(title = "是否使用https", example = "false")
    @NotNull
    private Boolean secure = Boolean.FALSE;
}