package com.snowdrift.pay.allin.properties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

/**
 * AllinBrandProperties
 *
 * @author gaoye
 * @date 2025/05/29 13:10:17
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "pay.allin.brand")
public class AllinBrandProperties implements Serializable {

    @Schema(title = "是否启用")
    @NotNull(message = "必须指定是否启用")
    private Boolean enabled = Boolean.FALSE;

    @Schema(title = "是否测试")
    @NotNull(message = "必须指定是否测试")
    public Boolean testMode = Boolean.FALSE;

    @Schema(title = "智品牌接口测试地址")
    public String testUrl = "https://tlgd-test.allinpaygd.com/ug/gateway.do";

    @Schema(title = "智品牌接口地址")
    @NotBlank(message = "智品牌接口地址不能为空")
    private String prodUrl = "https://cloud.aipgd.com/gateway";

    @Schema(title = "应用ID")
    @NotBlank(message = "APPID不能为空")
    private String appId = "test";

    @Schema(title = "应用Key")
    @NotBlank(message = "APPKEY不能为空")
    public String appKey = "123456";

    @Schema(title = "版本号")
    @NotBlank(message = "版本号不能为空")
    public String version = "1.0";
}