package com.snowdrift.framework.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * MQ Binder 激活守卫 — 同一时间只允许启用一种 binder 实现模块
 * <p>
 * {@code IMqService} 由各 binder 实现模块通过 {@code @ConditionalOnMissingBean} 提供，
 * 若同时启用多种（如 kafka + rabbitmq），实现 Bean 的选择将不可确定。本守卫在启动时检测并失败退出。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqBinderActivationGuard implements InitializingBean, EnvironmentAware {

    private static final List<String> BINDER_KEYS = List.of("kafka", "rabbitmq", "rocketmq");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> enabled = new ArrayList<>();
        for (String key : BINDER_KEYS) {
            String value = environment.getProperty("snowdrift.mq." + key + ".enabled", "false");
            if (Boolean.parseBoolean(value)) {
                enabled.add("snowdrift.mq." + key + ".enabled=true");
            }
        }
        if (enabled.size() > 1) {
            throw new IllegalStateException("同一时间只允许启用一种 MQ binder，当前同时启用: " + enabled
                    + "。IMqService 实现将由多个 binder 竞争注册，请只保留其中一个。");
        }
        if (enabled.size() == 1) {
            log.info("MQ binder 激活: {}", enabled.get(0));
        }
    }
}
