package com.snowdrift.framework.schedule.quartz.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Quartz 调度配置属性
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
@Valid
@ConfigurationProperties(prefix = "snowdrift.schedule.quartz")
public class QuartzProperties {

    /**
     * 是否启用 Quartz
     */
    @NotNull
    private Boolean enabled = true;
}
