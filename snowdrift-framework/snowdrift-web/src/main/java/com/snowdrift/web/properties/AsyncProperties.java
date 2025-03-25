package com.snowdrift.web.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * AsyncProperties
 *
 * @author gaoye
 * @date 2025/03/24 14:11:18
 * @description 异步线程池配置
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.async")
public class AsyncProperties implements Serializable {

    /**
     * 是否开启异步
     */
    @NotNull(message = "是否开启异步不能为空")
    private Boolean enabled = Boolean.FALSE;

    /**
     * 线程池核心线程数
     */
    @NotNull(message = "线程池核心线程数不能为空")
    @Min(value = 1,message = "线程池核心线程数不能小于1")
    private Integer corePoolSize = 5;

    /**
     * 线程池最大线程数
     */
    @NotNull(message = "线程池最大线程数不能为空")
    @Min(value = 1,message = "线程池最大线程数不能小于1")
    private Integer maxPoolSize = 10;

    /**
     * 队列容量
     */
    @NotNull(message = "队列容量不能为空")
    @Min(value = 1,message = "队列容量不能小于1")
    private Integer queueCapacity = 256;

    /**
     * 线程名前缀
     */
    @NotBlank(message = "线程名前缀不能为空")
    private String threadNamePrefix = "async-";

    /**
     * 线程池关闭时等待任务完成
     */
    @NotNull(message = "线程池关闭时等待任务完成不能为空")
    private Boolean waitForTasksToCompleteOnShutdown = Boolean.TRUE;

    /**
     * 等待任务在关机时完成，并设置超时时间
     */
    @NotNull(message = "等待任务在关机时完成，并设置超时时间不能为空")
    private Integer awaitTerminationSeconds = 60;
}