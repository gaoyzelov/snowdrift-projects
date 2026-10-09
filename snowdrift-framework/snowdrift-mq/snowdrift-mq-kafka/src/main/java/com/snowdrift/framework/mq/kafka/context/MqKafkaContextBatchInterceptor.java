package com.snowdrift.framework.mq.kafka.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;

/**
 * Kafka 批消费的上下文清理拦截器
 * <p>
 * 挂在 {@code @KafkaListener(batchListener = true)} 的监听容器上。与记录级拦截器
 * （{@link MqKafkaContextRecordInterceptor}）不同，这里<b>刻意不做上下文还原</b>：
 * </p>
 * <ul>
 *   <li>一个批次含多条记录、各自带有消息头，而上下文载体是 {@code ThreadLocal}，无法表达多个上下文；</li>
 *   <li>若按批次内第一条记录还原，监听方法会把整批消费都归到第一条记录的用户/租户名下，
 *       审计与数据权限随之出错，比不还原更糟；</li>
 *   <li>且首条记录签名校验失败会使 {@code restore} 抛异常，导致<b>整批中止</b>。</li>
 * </ul>
 * <p>
 * 因此本拦截器职责限定为<b>保证不泄漏</b>：批次开始前与批次结束（成功/失败）时清理上下文，
 * 避免容器线程复用造成上下文串批。
 * </p>
 * <p>
 * 批监听若确需按记录取上下文，请在监听方法内自行还原并及时清理，例如：
 * </p>
 * <pre>{@code
 * for (ConsumerRecord<String, byte[]> record : records) {
 *     contextPropagator.restore(KafkaHeaderCodec.toMap(record.headers()));
 *     try {
 *         // 业务处理
 *     } finally {
 *         contextPropagator.clear();
 *     }
 * }
 * }</pre>
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @since 1.0.0
 */
public class MqKafkaContextBatchInterceptor implements BatchInterceptor<String, byte[]> {

    private final MqContextPropagator contextPropagator;

    public MqKafkaContextBatchInterceptor(MqContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public ConsumerRecords<String, byte[]> intercept(ConsumerRecords<String, byte[]> records,
                                                     Consumer<String, byte[]> consumer) {
        // 批次开始前先清理：杜绝上一批（或上一请求）残留在本线程上的上下文被本批读到
        contextPropagator.clear();
        return records;
    }

    @Override
    public void success(ConsumerRecords<String, byte[]> records, Consumer<String, byte[]> consumer) {
        contextPropagator.clear();
    }

    @Override
    public void failure(ConsumerRecords<String, byte[]> records, Exception exception,
                        Consumer<String, byte[]> consumer) {
        contextPropagator.clear();
    }
}
