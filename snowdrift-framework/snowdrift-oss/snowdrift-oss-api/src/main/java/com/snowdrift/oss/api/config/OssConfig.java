package com.snowdrift.oss.api.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
@ApiModel(value = "OssConfig", description = "OSS配置")
public class OssConfig implements Serializable {

    @NotBlank(message = "endpoint不能为空")
    @ApiModelProperty(value = "OSS服务端点", example = "oss-cn-beijing.aliyuncs.com", position = 1)
    private String endpoint;

    @ApiModelProperty(value = "访问秘钥", example = "4a96sd7f89s7df9as7df", position = 2)
    @NotBlank(message = "accessKey不能为空")
    private String accessKey;

    @ApiModelProperty(value = "安全秘钥", example = "4a96sd7f89s7df9as7df", position = 3)
    @NotBlank(message = "secretKey不能为空")
    private String secretKey;

    @ApiModelProperty(value = "存储桶", example = "test", position = 4)
    private String bucket;

    @ApiModelProperty(value = "区域", example = "cn-beijing", position = 5)
    private String region;

    @ApiModelProperty(value = "域名", example = "https://test.oss-cn-beijing.aliyuncs.com", position = 6)
    private String domain;

    @ApiModelProperty(value = "是否使用https", example = "false", position = 7)
    @NotNull
    private Boolean secure = Boolean.FALSE;
}