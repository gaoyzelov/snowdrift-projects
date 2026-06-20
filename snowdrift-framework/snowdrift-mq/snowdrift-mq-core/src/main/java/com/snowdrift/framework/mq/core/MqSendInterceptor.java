package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.mq.dto.MqSendResult;

/**
 * 消息发送拦截器 SPI
 * <p>
 * 在消息发送前后插入自定义逻辑，支持加密、审计、限流、格式转换等场景。
 * 实现此接口并注册为 Spring Bean 即可自动生效。
 * </p>
 *
 * <pre>{@code
 * @Component
 * public class AuditInterceptor implements MqSendInterceptor {
 *     public void beforeSend(String topic, String key, Object payload) {
 *         log.info("发送前审计: topic={}, payload={}", topic, payload);
 *     }
 *     public void afterSend(String topic, MqSendResult result) {
 *         log.info("发送后审计: topic={}, result={}", topic, result);
 *     }
 *     public void onSendError(String topic, Throwable ex) {
 *         log.error("发送失败审计: topic={}", topic, ex);
 *     }
 * }
 * }</pre>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
public interface MqSendInterceptor {

    /**
     * 拦截器优先级，数值越大越先执行，默认 0
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 发送前回调
     *
     * @param topic   目标 topic
     * @param key     消息 Key（可空）
     * @param payload 消息体
     */
    default void beforeSend(String topic, String key, Object payload) {
    }

    /**
     * 发送成功后回调
     *
     * @param topic  目标 topic
     * @param result 发送结果
     */
    default void afterSend(String topic, MqSendResult result) {
    }

    /**
     * 发送失败后回调
     *
     * @param topic 目标 topic
     * @param ex    异常
     */
    default void onSendError(String topic, Throwable ex) {
    }
}
