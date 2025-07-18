package com.snowdrift.doc.knife4j.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.List;

/**
 * Knife4jApiDocProperties
 *
 * @author gaoye
 * @date 2025/03/18 17:43:30
 * @description Knife4j API接口文档配置
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.doc.knife4j")
public class Knife4jApiDocProperties implements Serializable {

    /**
     * 是否启用
     */
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled = Boolean.FALSE;

    /**
     * 文档分组
     */
    @NotBlank(message = "文档分组不能为空")
    private String group = "v1";

    /**
     * 文档显示名称
     */
    private String displayName;

    /**
     * 文档扫描包
     */
    @NotEmpty(message = "文档扫描包不能为空")
    private List<String> basePackages;

    /**
     * 文档标题
     */
    @NotBlank(message = "文档标题不能为空")
    private String title = "Snowdrift";

    /**
     * 文档描述
     */
    @NotBlank(message = "文档描述不能为空")
    private String description = "A Snowdrift Project";

    /**
     * 系统网站
     */
    @NotBlank(message = "系统网站不能为空")
    private String website = "https://github.com/gaoyzelov/snowdrift-projects";

    /**
     * 文档版本
     */
    @NotBlank(message = "文档版本不能为空")
    private String version = "1.0.0";

}