package com.snowdrift.framework.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 静态资源配置属性
 *
 * @author 83674
 * @date 2026/5/9
 * @description 静态资源映射配置，支持本地文件访问
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.resource")
public class ResourceProperties {

    /**
     * 是否启用静态资源映射
     */
    private Boolean enabled = false;

    /**
     * 静态资源映射列表
     */
    private List<ResourceMapping> mappings = new ArrayList<>();

    /**
     * 静态资源映射配置
     */
    @Data
    public static class ResourceMapping {

        /**
         * 访问路径前缀
         * 示例：/files/**
         */
        private String pathPattern;

        /**
         * 资源位置（本地文件路径）
         * 示例：file:D:/data/oss-storage/
         * 示例：file:/opt/oss-storage/
         */
        private String location;

        /**
         * 缓存控制（秒）
         * 0: 不缓存
         * -1: 使用默认缓存
         * >0: 缓存指定秒数
         */
        private Integer cachePeriod = 3600;

        /**
         * 是否使用缓存控制
         */
        private Boolean useCacheControl = true;
    }
}
