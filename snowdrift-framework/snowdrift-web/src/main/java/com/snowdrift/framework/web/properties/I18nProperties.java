package com.snowdrift.framework.web.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * I18nProperties
 *
 * @author 83674
 * @date 2026/5/9
 * @description 国际化配置属性
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.i18n")
public class I18nProperties implements Serializable {

    /**
     * 是否开启国际化
     */
    @NotNull
    private Boolean enabled = Boolean.TRUE;

    /**
     * 默认语言
     */
    @NotBlank
    private String defaultLocale = "zh_CN";

    /**
     * 参数名（优先级低，请求头不存在时使用）
     */
    @NotBlank
    private String paramName = "lang";

    /**
     * 缓存时间（秒）
     * -1 表示永不缓存（开发环境推荐）
     * 0 表示每次都需要重新加载
     * 正数表示缓存指定秒数（生产环境推荐，如：3600）
     */
    @NotNull
    private Integer cacheSeconds = -1;

    /**
     * 支持的语言列表
     */
    @NotEmpty
    private List<String> supportedLocales = List.of("zh_CN", "en_US");

    /**
     * 资源文件基础名称
     */
    @NotEmpty
    private Set<String> baseNames = Set.of("i18n/web-messages", "i18n/oss-messages", "i18n/security-messages", "i18n/cache-messages");

    /**
     * 编码格式
     */
    @NotBlank
    private String encoding = "UTF-8";

    /**
     * 是否使用代码作为默认消息
     */
    @NotNull
    private Boolean useCodeAsDefaultMessage = Boolean.TRUE;
}
