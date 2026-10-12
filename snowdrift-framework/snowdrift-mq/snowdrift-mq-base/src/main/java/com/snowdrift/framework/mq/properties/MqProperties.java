package com.snowdrift.framework.mq.properties;

import jakarta.validation.Valid;
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
    private Boolean enabled = Boolean.TRUE;

    /**
     * 是否对消息中的身份头做 HMAC-SHA256 签名校验
     * <p><b>默认开启。</b>消费端会用消息头里的身份（userId / username / tenantId / deptId）覆盖当前线程的
     * {@code SecurityContext}，因此这些头在跨进程传递时必须有完整性保护：任何能向同一 topic/queue
     * 发布消息的一方都能伪造它们。关闭后消费端将无条件信任未签名的身份头，等同于把身份认定权交给生产者——
     * 仅在你完全信任所有生产者且有网络隔离时才可关闭。</p>
     * <p>开启时 {@code sign-key} 必填，否则启动即失败（避免生产者不签名 / 消费者全量拒收的错误组合）。</p>
     */
    private Boolean sign = Boolean.TRUE;

    /**
     * 消息签名密钥（{@code sign=true} 时必填）
     * <p>需在所有生产端与消费端之间共享同一密钥；建议通过环境变量或配置中心注入，不要明文入库。</p>
     */
    private String signKey;

    /**
     * 异步发送线程池配置
     */
    @Valid
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

        /**
         * 关闭时是否等待任务完成
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 关闭等待超时（秒）
         */
        private int awaitTerminationSeconds = 30;
    }
}
