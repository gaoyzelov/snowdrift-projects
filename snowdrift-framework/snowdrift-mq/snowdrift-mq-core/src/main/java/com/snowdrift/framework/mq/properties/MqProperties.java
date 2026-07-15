package com.snowdrift.framework.mq.properties;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息队列配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.mq")
public class MqProperties {

    /**
     * 是否启用消息队列功能
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * 动态目的地缓存大小（StreamBridge 内部缓存）
     * 默认10，每个未预声明的 topic 会占用一个缓存槽位
     */
    @NotNull
    private Integer dynamicDestinationCacheSize = 10;

    /**
     * 消息签名配置
     */
    private Boolean sign = Boolean.FALSE;

    /**
     * 消息签名密钥
     */
    private String signKey;

    /**
     * 异步发送线程池配置
     */
    @Validated
    private ExecutorProperties executor = new ExecutorProperties();

    /**
     * 异步发送线程池配置
     */
    @Data
    public static class ExecutorProperties {

        /**
         * 核心线程数
         */
        @Min(1)
        private int coreSize = 4;

        /**
         * 最大线程数
         */
        @Min(1)
        private int maxSize = 8;

        /**
         * 队列容量
         */
        @Min(0)
        private int queueCapacity = 100;

        /**
         * 线程存活时间（秒）
         */
        @Min(1)
        private int keepAliveSeconds = 60;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "snowdrift-mq-async-";
    }
}
