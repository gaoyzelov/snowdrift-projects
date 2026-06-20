package com.snowdrift.framework.mq.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MQ 消息监听器注解
 * <p>
 * 标注在 Spring Bean 的方法上，自动注册为 Spring Cloud Stream Consumer。
 * 框架会在启动时将 {@code @MqListener} 方法包装为 {@code Consumer<Message<byte[]>>}，
 * 自动处理消息反序列化、上下文恢复和异常翻译。
 * </p>
 *
 * <pre>{@code
 * @Component
 * public class OrderListener {
 *     @MqListener(topic = "order-paid", group = "order-service")
 *     public void onOrderPaid(OrderPaidEvent event) {
 *         // SecurityContext.getUserId() 自动恢复为发送方的 userId
 *         orderService.process(event);
 *     }
 * }
 * }</pre>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqListener {

    /**
     * 监听的 topic / destination 名称
     */
    String topic();

    /**
     * 消费组（可选）
     * — Kafka: consumer group.id
     * — RocketMQ: consumer group
     * — RabbitMQ: queue group
     */
    String group() default "";

    /**
     * 最大重试次数，默认 3
     * — 0 = 不重试（消费失败即丢弃 / 进入 DLQ）
     */
    int maxRetry() default 3;

    /**
     * 消费线程并发数，默认 1
     */
    int concurrency() default 1;

    /**
     * 是否自动提交偏移量，默认 false
     * — Kafka: enable.auto.commit
     */
    boolean autoCommit() default false;
}
