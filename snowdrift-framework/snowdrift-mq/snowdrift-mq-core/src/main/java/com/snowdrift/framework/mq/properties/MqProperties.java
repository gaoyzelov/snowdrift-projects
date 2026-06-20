package com.snowdrift.framework.mq.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息队列配置属性
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Data
@Valid
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
}
