package com.snowdrift.framework.mq.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQ 拦截器注册表 — 支持运行时动态增删，按 {@link MqSendInterceptor#getPriority()} 降序执行
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqInterceptorRegistry {

    private final CopyOnWriteArrayList<MqSendInterceptor> interceptors = new CopyOnWriteArrayList<>();

    /**
     * 注册拦截器（运行时动态添加）
     */
    public synchronized void register(MqSendInterceptor interceptor) {
        if (interceptor == null) return;
        interceptors.add(interceptor);
        interceptors.sort(Comparator.comparingInt(MqSendInterceptor::getPriority).reversed());
        log.debug("拦截器已注册: {}, priority={}", interceptor.getClass().getName(), interceptor.getPriority());
    }

    /**
     * 移除拦截器
     */
    public void unregister(MqSendInterceptor interceptor) {
        if (interceptor == null) return;
        interceptors.remove(interceptor);
    }

    /**
     * 获取排序后的拦截器快照（按优先级降序，同优先级按注册先后）
     */
    public List<MqSendInterceptor> getInterceptors() {
        return new ArrayList<>(interceptors);
    }
}
