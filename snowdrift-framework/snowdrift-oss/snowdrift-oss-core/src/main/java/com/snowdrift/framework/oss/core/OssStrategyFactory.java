package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.exception.OssException;
import lombok.Setter;
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
     * -- SETTER --
     *  设置默认配置标识
     *  <p>
     *  设置默认 OSS 实例的配置标识
     *  调用 getDefaultService() 时会返回该标识对应的实例
     *
     * @param defaultConfigKey 默认配置标识

     */
    @Setter
    private String defaultConfigKey = "default";
    
    /**
     * 注册 OSS 实例
     * <p>
     * 将 OSS Service 实例注册到工厂中，后续可通过 configKey 获取
     * 如果 configKey 已存在，会覆盖旧实例
     *
     * @param configKey 配置标识，如 default、backup 等，不能为空
     * @param service   OSS Service 实例，不能为空
     * @throws OssException 当 configKey 为空或 service 为空时抛出
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
     * <p>
     * 根据配置标识获取已注册的 OSS Service 实例
     *
     * @param configKey 配置标识
     * @return OSS Service 实例
     * @throws OssException 当 configKey 不存在时抛出
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
     * <p>
     * 获取默认配置的 OSS Service 实例
     * 默认配置标识可通过 setDefaultConfigKey() 设置，默认为 "default"
     *
     * @return 默认 OSS Service 实例
     * @throws OssException 当默认配置不存在时抛出
     */
    public IOssService getDefaultService() {
        return getService(defaultConfigKey);
    }
    
    /**
     * 移除 OSS 实例
     * <p>
     * 从工厂中移除指定配置标识的 OSS Service 实例
     * 如果 configKey 不存在，不会抛出异常
     *
     * @param configKey 配置标识
     */
    public void remove(String configKey) {
        serviceMap.remove(configKey);
        log.info("移除 OSS 实例: configKey={}", configKey);
    }
    
    /**
     * 检查是否存在指定配置
     * <p>
     * 判断指定配置标识的 OSS Service 实例是否已注册
     *
     * @param configKey 配置标识
     * @return true 如果配置存在，false 如果不存在
     */
    public boolean contains(String configKey) {
        return serviceMap.containsKey(configKey);
    }
    
    /**
     * 获取所有 OSS 实例
     * <p>
     * 返回所有已注册的 OSS Service 实例的副本
     * 返回的 Map 是线程安全的副本，修改不会影响原数据
     *
     * @return 配置标识到 OSS Service 实例的映射
     */
    public Map<String, IOssService> getAllServices() {
        return new ConcurrentHashMap<>(serviceMap);
    }

    /**
     * 根据配置动态创建并注册 OSS 实例
     * <p>
     * 使用 ServiceCreator 创建 OSS Service 实例并注册到工厂
     * 适用于从数据库或其他配置源动态加载 OSS 配置的场景
     * 如果配置标记为默认配置，会自动更新默认配置标识
     *
     * @param config         OSS 配置信息
     * @param serviceCreator Service 创建器，用于创建具体的 OSS Service 实例
     * @throws OssException 当 config 为空时抛出
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
     * <p>
     * 先移除旧的 OSS Service 实例，再根据新配置创建并注册新实例
     * 适用于运行时动态更新 OSS 配置的场景，无需重启应用
     *
     * @param configKey      配置标识
     * @param config         新的 OSS 配置信息
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
     * <p>
     * 函数式接口，用于创建 OSS Service 实例
     * 各存储类型实现模块（如 MinIO、阿里云等）需提供对应的创建器
     */
    @FunctionalInterface
    public interface ServiceCreator {
        
        /**
         * 创建 OSS Service 实例
         *
         * @param config OSS 配置信息
         * @return OSS Service 实例
         */
        IOssService create(OssConfigDTO config);
    }
}
