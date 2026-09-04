package com.snowdrift.framework.mq.rabbitmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;

import java.util.Map;

/**
 * RabbitMQ 消费上下文自动恢复/清理 Advice（容器级）
 * <p>
 * 通过容器工厂 {@code adviceChain} 应用到每个 {@code @RabbitListener} 监听：
 * 在监听方法调用前从 AMQP {@link Message} 头恢复 traceId / SecurityContext，
 * 调用结束（成功或异常）后在 finally 中清理，杜绝容器线程上下文残留。
 * 签名不通过时 restore 抛异常且已自行清理，异常上抛交由容器错误处理（重试/DLQ）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
public class MqRabbitContextAdvice implements MethodInterceptor {

    private final MqContextPropagator contextPropagator;

    public MqRabbitContextAdvice(MqContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = findMessageArgument(invocation.getArguments());
        if (message != null) {
            Map<String, String> headers = RabbitHeaderCodec.toMap(message.getMessageProperties());
            contextPropagator.restore(headers);
        }
        try {
            return invocation.proceed();
        } finally {
            contextPropagator.clear();
        }
    }

    private Message findMessageArgument(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            if (argument instanceof Message message) {
                return message;
            }
        }
        return null;
    }
}
