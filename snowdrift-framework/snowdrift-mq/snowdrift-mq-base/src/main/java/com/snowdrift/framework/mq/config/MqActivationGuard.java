package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.mq.exception.MqException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * MQ 实现模块互斥守卫
 * <p>
 * snowdrift-mq 的三种实现（Kafka / RabbitMQ / RocketMQ）在语义上互斥：{@code IMqService} 门面
 * 以 {@code @ConditionalOnMissingBean} 注册，若同时启用多个，注入的实现取决于自动配置评估顺序（不保证），
 * 而各实现的消费端容器钩子又会同时注册，导致「发送用 A、消费钩子挂 B/C」的不一致，且难以排查。
 * </p>
 * <p>
 * 本守卫在容器启动阶段读取 {@code snowdrift.mq.<vendor>.enabled} 开关，一旦发现启用数 &gt; 1 立即失败，
 * 把「配置错误」暴露在启动期而非运行期。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @since 1.0.0
 */
@Slf4j
public class MqActivationGuard {

    /** 三种实现模块的启用开关（与各实现模块 {@code @ConditionalOnProperty} 前缀保持一致） */
    private static final List<String> VENDOR_ENABLED_KEYS = List.of(
            "snowdrift.mq.kafka.enabled",
            "snowdrift.mq.rabbitmq.enabled",
            "snowdrift.mq.rocketmq.enabled");

    public MqActivationGuard(Environment environment) {
        List<String> enabled = new ArrayList<>();
        for (String key : VENDOR_ENABLED_KEYS) {
            if (environment.getProperty(key, Boolean.class, Boolean.FALSE)) {
                enabled.add(key);
            }
        }
        if (enabled.size() > 1) {
            throw new MqException(String.format(
                    "snowdrift-mq 同一时间只允许启用一种实现，当前启用了 %d 个：%s。请仅保留一个 snowdrift.mq.<vendor>.enabled=true",
                    enabled.size(), enabled));
        }
        if (enabled.size() == 1) {
            log.debug("snowdrift-mq 已启用实现: {}", enabled.get(0));
        }
    }
}
