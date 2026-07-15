package com.snowdrift.framework.web.properties;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * AsyncProperties
 *
 * @author gaoyzelov
 * @date 2026/5/8-15:29
 * @description 异步配置
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.async")
public class AsyncProperties implements Serializable {

    /**
     * 是否开启异步
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * 线程池核心线程数
     */
    @NotNull
    @Min(1)
    private Integer corePoolSize = 2;

    /**
     * 线程池最大线程数
     */
    @NotNull
    @Min(1)
    private Integer maxPoolSize = 10;

    /**
     * 队列容量
     */
    @NotNull
    private Integer queueCapacity = 256;

    /**
     * 线程名前缀
     */
    @NotBlank
    private String threadNamePrefix = "async-";

    /**
     * 线程池关闭时等待任务完成
     */
    @NotNull
    private Boolean waitForTasksToCompleteOnShutdown = Boolean.TRUE;

    /**
     * 等待任务在关机时完成，并设置超时时间
     */
    @Min(1)
    @NotNull
    private Integer awaitTerminationSeconds = 60;
}
