package com.snowdrift.framework.schedule.quartz.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Quartz 调度配置属性
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.schedule.quartz")
public class QuartzProperties {

    /**
     * 是否启用 Quartz
     */
    @NotNull
    private Boolean enabled = true;
}
