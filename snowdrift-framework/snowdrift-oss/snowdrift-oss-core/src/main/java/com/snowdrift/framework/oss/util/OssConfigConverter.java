package com.snowdrift.framework.oss.util;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.properties.OssInstanceProperties;

/**
 * OSS 配置转换器
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description 用于 OssProperties 和 OssConfigDTO 之间的转换
 * @since 1.0.0
 */
public class OssConfigConverter {
    
    /**
     * 将 OssInstanceProperties 转换为 OssConfigDTO
     *
     * @param properties YAML 配置属性
     * @param configKey  配置标识
     * @return OssConfigDTO
     */
    public static OssConfigDTO fromProperties(OssInstanceProperties properties, String configKey) {
        if (properties == null) {
            return null;
        }
        
        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey(configKey);
        config.setOssType(properties.getOssType());
        config.setEndpoint(properties.getEndpoint());
        config.setAccessKey(properties.getAccessKey());
        config.setSecretKey(properties.getSecretKey());
        config.setDomain(properties.getDomain());
        config.setBucket(properties.getBucket());
        config.setRegion(properties.getRegion());
        config.setUrlStyle(properties.getUrlStyle());
        config.setPathPrefix(properties.getPathPrefix());
        config.setPrivateBucket(properties.getPrivateBucket());
        config.setSignatureExpiry(properties.getSignatureExpiry());
        config.setChunkSize(properties.getChunkSize());
        config.setUploadTokenExpire(properties.getUploadTokenExpire());
        config.setChunkUploadUrlExpire(properties.getChunkUploadUrlExpire());
        config.setOrphanFileCleanupDays(properties.getOrphanFileCleanupDays());
        config.setMultipartCleanupHours(properties.getMultipartCleanupHours());
        config.setEnabled(properties.getEnabled());
        config.setIsDefault(properties.getIsDefault());
        config.setRemark(properties.getRemark());
        
        return config;
    }
    
    /**
     * 将 OssConfigDTO 转换为 OssInstanceProperties
     *
     * @param config OSS 配置 DTO
     * @return OssInstanceProperties
     */
    public static OssInstanceProperties toProperties(OssConfigDTO config) {
        if (config == null) {
            return null;
        }
        
        OssInstanceProperties properties = new OssInstanceProperties();
        // 注意：OssInstanceProperties 没有 configKey 字段，因此不设置
        properties.setOssType(config.getOssType());
        properties.setEndpoint(config.getEndpoint());
        properties.setAccessKey(config.getAccessKey());
        properties.setSecretKey(config.getSecretKey());
        properties.setDomain(config.getDomain());
        properties.setBucket(config.getBucket());
        properties.setRegion(config.getRegion());
        properties.setUrlStyle(config.getUrlStyle());
        properties.setPathPrefix(config.getPathPrefix());
        properties.setPrivateBucket(config.getPrivateBucket());
        properties.setSignatureExpiry(config.getSignatureExpiry());
        properties.setChunkSize(config.getChunkSize());
        properties.setUploadTokenExpire(config.getUploadTokenExpire());
        properties.setChunkUploadUrlExpire(config.getChunkUploadUrlExpire());
        properties.setOrphanFileCleanupDays(config.getOrphanFileCleanupDays());
        properties.setMultipartCleanupHours(config.getMultipartCleanupHours());
        properties.setEnabled(config.getEnabled());
        properties.setIsDefault(config.getIsDefault());
        properties.setRemark(config.getRemark());
        
        return properties;
    }
}
