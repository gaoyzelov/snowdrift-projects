package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.properties.OssProperties;
import com.snowdrift.framework.oss.util.OssConfigConverter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS 策略工厂
 *
 * @author gaoyzelov
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
     * 默认配置标识（使用 volatile 保证可见性）
     */
    private volatile String defaultConfigKey = "default";
    
    /**
     * 设置默认配置标识
     * <p>
     * 设置默认 OSS 实例的配置标识
     * 调用 getDefaultService() 时会返回该标识对应的实例
     *
     * @param defaultConfigKey 默认配置标识
     */
    public void setDefaultConfigKey(String defaultConfigKey) {
        this.defaultConfigKey = defaultConfigKey;
    }
    
    /**
     * 获取默认配置标识
     *
     * @return 默认配置标识
     */
    public String getDefaultConfigKey() {
        return defaultConfigKey;
    }
    
    /**
     * 注册 OSS 实例
     *
     * @param configKey 配置标识，如 default、backup 等，不能为空
     * @param service   OSS Service 实例，不能为空
     * @throws OssException 当 configKey 为空或 service 为空时抛出
     */
    public synchronized void register(String configKey, IOssService service) {
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
    public synchronized IOssService getService(String configKey) {
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
     * 移除前会先调用 close() 释放资源（如 HTTP 连接池）
     * 如果 configKey 不存在，不会抛出异常
     *
     * @param configKey 配置标识
     */
    public synchronized void remove(String configKey) {
        IOssService service = serviceMap.remove(configKey);
        if (service != null) {
            try {
                service.close();
                log.info("移除并关闭 OSS 实例: configKey={}, type={}", configKey, service.getType());
            } catch (Exception e) {
                log.warn("关闭 OSS 实例失败: configKey={}", configKey, e);
            }
        } else {
            log.warn("移除 OSS 实例失败，实例不存在: configKey={}", configKey);
        }
    }
    
    /**
     * 检查是否存在指定配置
     * <p>
     * 判断指定配置标识的 OSS Service 实例是否已注册
     *
     * @param configKey 配置标识
     * @return true 如果配置存在，false 如果不存在
     */
    public synchronized boolean contains(String configKey) {
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
    public synchronized Map<String, IOssService> getAllServices() {
        return new ConcurrentHashMap<>(serviceMap);
    }

    /**
     * 根据配置动态创建并注册 OSS 实例
     * <p>
     * 使用 ServiceCreator 创建 OSS Service 实例并注册到工厂
     * 适用于从数据库或其他配置源动态加载 OSS 配置的场景
     * 如果配置标记为默认配置，会自动更新默认配置标识
     * <p>
     * 注意：该方法使用 synchronized 保证线程安全，避免并发注册时的竞态条件
     *
     * @param config         OSS 配置信息
     * @param serviceCreator Service 创建器，用于创建具体的 OSS Service 实例
     * @throws OssException 当 config 为空时抛出
     */
    public synchronized void registerFromConfig(OssConfigDTO config, ServiceCreator serviceCreator) {
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
     * 从 OssProperties 批量注册指定类型的 OSS 实例
     * <p>
     * 遍历配置中的所有实例，过滤出匹配目标类型的启用配置，
     * 转换为 OssConfigDTO 后通过 ServiceCreator 创建并注册到工厂。
     * 各存储类型实现模块（MinIO、阿里云等）在 @PostConstruct 中调用此方法即可。
     * </p>
     *
     * @param ossProperties   OSS 配置（YAML 或数据库来源）
     * @param targetType      目标存储类型
     * @param serviceCreator  Service 创建器
     */
    public synchronized void registerFromProperties(OssProperties ossProperties, OssTypeEnum targetType,
                                        ServiceCreator serviceCreator) {
        if (MapUtils.isEmpty(ossProperties.getConfigs())) {
            log.debug("未配置 OSS 实例，跳过 {} 存储注册", targetType.getNote());
            return;
        }

        ossProperties.getConfigs().forEach((configKey, props) -> {
            if (targetType == props.getOssType() && props.getEnabled()) {
                try {
                    OssConfigDTO config = OssConfigConverter.fromProperties(props, configKey);
                    register(configKey, serviceCreator.create(config));
                    log.info("{} 存储实例注册成功: configKey={}", targetType.getNote(), configKey);
                } catch (Exception e) {
                    log.error("{} 存储实例注册失败: configKey={}", targetType.getNote(), configKey, e);
                }
            }
        });
    }

    /**
     * 重新加载配置（热更新）
     * <p>
     * 先移除旧的 OSS Service 实例，再根据新配置创建并注册新实例
     * 适用于运行时动态更新 OSS 配置的场景，无需重启应用
     * <p>
     * 注意：该方法使用同步锁保证线程安全，在同一个锁内完成移除和注册，
     * 避免 reload 期间其他线程获取到不存在的实例
     *
     * @param configKey      配置标识
     * @param config         新的 OSS 配置信息
     * @param serviceCreator Service 创建器
     */
    public synchronized void reload(String configKey, OssConfigDTO config, ServiceCreator serviceCreator) {
        if (config == null) {
            throw new OssException("oss.config.null");
        }

        // 移除旧实例
        IOssService oldService = serviceMap.remove(configKey);
        // 关闭旧实例
        if (oldService != null) {
            try {
                oldService.close();
                log.info("关闭旧 OSS 实例: configKey={}", configKey);
            } catch (Exception e) {
                log.warn("关闭旧 OSS 实例失败: configKey={}", configKey, e);
            }
        }
        


        // 注册新实例（在同步块内）
        if (Boolean.TRUE.equals(config.getEnabled())) {
            IOssService newService = serviceCreator.create(config);
            serviceMap.put(config.getConfigKey(), newService);
            log.info("注册新 OSS 实例: configKey={}, type={}", config.getConfigKey(), newService.getType());
            
            // 如果是默认配置，更新默认标识
            if (Boolean.TRUE.equals(config.getIsDefault())) {
                setDefaultConfigKey(config.getConfigKey());
                log.info("设置默认 OSS 配置: configKey={}", config.getConfigKey());
            }
        } else {
            log.warn("OSS 配置未启用，跳过注册: configKey={}", configKey);
        }
        
        log.info("热更新 OSS 配置完成: configKey={}", configKey);
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

    /**
     * 应用关闭时释放所有 OSS 客户端资源
     * <p>
     * 在 Spring 容器销毁前调用，遍历所有已注册的 OSS Service 实例
     * 调用其 close() 方法释放底层连接资源（如 HTTP 连接池等）
     * 单个实例关闭失败不会影响其他实例的关闭
     */
    @PreDestroy
    public synchronized void destroy() {
        if (serviceMap.isEmpty()) {
            log.debug("无 OSS 实例需要关闭");
            return;
        }

        log.info("开始关闭所有 OSS 客户端，共 {} 个实例", serviceMap.size());
        
        serviceMap.forEach((configKey, service) -> {
            try {
                service.close();
                log.info("OSS 客户端已关闭: configKey={}, type={}", configKey, service.getType());
            } catch (Exception e) {
                log.error("关闭 OSS 客户端失败: configKey={}, type={}", configKey, service.getType(), e);
            }
        });
        
        log.info("所有 OSS 客户端关闭完成");
    }
}
