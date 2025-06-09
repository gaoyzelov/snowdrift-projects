package com.snowdrift.web.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.List;

/**
 * CorsProperties
 *
 * @author gaoye
 * @date 2025/03/24 14:12:15
 * @description CORS跨域配置
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.web.cors")
public class CorsProperties implements Serializable {

    /**
     * 跨域配置列表
     */
    @Valid
    private List<CorsItem> corsItems;

    @Data
    public static class CorsItem {

        @NotBlank(message = "匹配路径不能为空")
        private String pathPattern;

        @NotNull(message = "是否允许证书不能为空")
        private Boolean allowCredentials;

        @NotNull(message = "允许的请求头不能为空")
        private String[] allowedHeaders;

        @NotNull(message = "允许的请求方法不能为空")
        private String[] allowedMethods;

        @NotNull(message = "允许的请求来源不能为空")
        private String[] allowedOriginPatterns;

        @NotNull(message = "最大请求缓存时间不能为空")
        private Long maxAge;
    }
}