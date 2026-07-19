package com.snowdrift.framework.web.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 防护配置属性
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.web.xss")
public class XssProperties {

    /**
     * 是否启用 XSS 防护
     */
    @NotNull
    private Boolean enabled = false;

    /**
     * 排除路径（Ant 风格，如 /admin/richtext/**），不进行 XSS 过滤
     */
    private List<String> excludePathPatterns = new ArrayList<>();
}
