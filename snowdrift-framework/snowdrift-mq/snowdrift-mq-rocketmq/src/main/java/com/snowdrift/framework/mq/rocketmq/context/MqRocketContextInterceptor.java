package com.snowdrift.framework.mq.rocketmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ 消费上下文自动恢复/清理拦截器（容器级）
 * <p>
 * 拦截 {@code DefaultRocketMQListenerContainer.handleMessage(MessageExt)}：
 * 调用前从 MessageExt.properties 恢复 traceId / SecurityContext（RocketMQTemplate 发送时已把头写入 properties），
 * 调用结束（成功或异常）在 finally 清理。签名不通过时 restore 抛异常且跳过业务调用，交由消费端重试处理。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
public class MqRocketContextInterceptor implements MethodInterceptor {

    private static final String HANDLE_MESSAGE = "handleMessage";

    private final MqContextPropagator contextPropagator;

    public MqRocketContextInterceptor(MqContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 只拦截消息处理入口，其余方法（生命周期等）原样透传
        if (!HANDLE_MESSAGE.equals(invocation.getMethod().getName())) {
            return invocation.proceed();
        }
        try {
            Object[] arguments = invocation.getArguments();
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof MessageExt messageExt) {
                contextPropagator.restore(messageExt.getProperties());
            }
            return invocation.proceed();
        } finally {
            contextPropagator.clear();
        }
    }
}
