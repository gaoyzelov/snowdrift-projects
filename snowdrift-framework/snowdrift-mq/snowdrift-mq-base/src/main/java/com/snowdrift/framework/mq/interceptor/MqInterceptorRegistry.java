package com.snowdrift.framework.mq.interceptor;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQ 拦截器注册表 — 支持运行时动态增删，按 {@link MqSendInterceptor#getPriority()} 降序执行
 * <p>写入线程安全（CopyOnWriteArrayList），排序在读时完成，避免每次注册都触发全量排序。</p>
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
    public void register(MqSendInterceptor interceptor) {
        if (interceptor == null) return;
        interceptors.add(interceptor);
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
     * 获取按优先级降序的拦截器快照（同优先级保持注册先后）
     */
    public List<MqSendInterceptor> getInterceptors() {
        List<MqSendInterceptor> snapshot = new ArrayList<>(interceptors);
        snapshot.sort(Comparator.comparingInt(MqSendInterceptor::getPriority).reversed());
        return snapshot;
    }
}
