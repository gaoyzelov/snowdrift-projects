package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.exception.OssException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS 策略工厂
 *
 * @author 83674
 * @date 2026/5/9
 * @description 管理和切换不同的 OSS 实例，支持动态注册和热更新
 * @since 1.0.0
 */
@Slf4j
public class OssStrategyFactory {
    
    /**
     * OSS 实例映射表
     * Key: 配置标识（如：default、backup）
     * Value: OSS Service 实例
     */
    private final Map<String, IOssService> serviceMap = new ConcurrentHashMap<>();
    
    /**
     * 默认配置标识
     */
    private String defaultConfigKey = "default";
    
    /**
     * 注册 OSS 实例
     *
     * @param configKey 配置标识
     * @param service   OSS Service
     */
    public void register(String configKey, IOssService service) {
        if (!StringUtils.hasText(configKey)) {
            throw new OssException("oss.config.key.empty");
        }
        if (service == null) {
            throw new OssException("oss.service.null");
        }
        
        serviceMap.put(configKey, service);
        log.info("注册 OSS 实例: configKey={}, type={}, bucket={}", 
            configKey, service.getType(), service.getBucket());
    }
    
    /**
     * 获取 OSS 实例
     *
     * @param configKey 配置标识
     * @return OSS Service
     */
    public IOssService getService(String configKey) {
        IOssService service = serviceMap.get(configKey);
        if (service == null) {
            throw new OssException("oss.config.not.found", new Object[]{configKey});
        }
        return service;
    }
    
    /**
     * 获取默认 OSS 实例
     *
     * @return OSS Service
     */
    public IOssService getDefaultService() {
        return getService(defaultConfigKey);
    }
    
    /**
     * 移除 OSS 实例
     *
     * @param configKey 配置标识
     */
    public void remove(String configKey) {
        serviceMap.remove(configKey);
        log.info("移除 OSS 实例: configKey={}", configKey);
    }
    
    /**
     * 检查是否存在指定配置
     *
     * @param configKey 配置标识
     * @return 是否存在
     */
    public boolean contains(String configKey) {
        return serviceMap.containsKey(configKey);
    }
    
    /**
     * 获取所有配置标识
     *
     * @return 配置标识集合
     */
    public Map<String, IOssService> getAllServices() {
        return new ConcurrentHashMap<>(serviceMap);
    }
    
    /**
     * 设置默认配置标识
     *
     * @param defaultConfigKey 默认配置标识
     */
    public void setDefaultConfigKey(String defaultConfigKey) {
        this.defaultConfigKey = defaultConfigKey;
    }
    
    /**
     * 根据配置动态创建并注册 OSS 实例
     *
     * @param config       OSS 配置
     * @param serviceCreator Service 创建器
     */
    public void registerFromConfig(OssConfigDTO config, ServiceCreator serviceCreator) {
        if (config == null) {
            throw new OssException("oss.config.null");
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            log.warn("OSS 配置未启用: configKey={}", config.getConfigKey());
            return;
        }
        
        IOssService service = serviceCreator.create(config);
        register(config.getConfigKey(), service);
        
        // 如果是默认配置，更新默认标识
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            setDefaultConfigKey(config.getConfigKey());
            log.info("设置默认 OSS 配置: configKey={}", config.getConfigKey());
        }
    }
    
    /**
     * 重新加载配置（热更新）
     *
     * @param configKey    配置标识
     * @param config       新配置
     * @param serviceCreator Service 创建器
     */
    public void reload(String configKey, OssConfigDTO config, ServiceCreator serviceCreator) {
        // 移除旧实例
        remove(configKey);
        
        // 注册新实例
        registerFromConfig(config, serviceCreator);
        
        log.info("热更新 OSS 配置: configKey={}", configKey);
    }
    
    /**
     * Service 创建器接口
     */
    @FunctionalInterface
    public interface ServiceCreator {
        
        /**
         * 创建 OSS Service
         *
         * @param config OSS 配置
         * @return OSS Service
         */
        IOssService create(OssConfigDTO config);
    }
}
