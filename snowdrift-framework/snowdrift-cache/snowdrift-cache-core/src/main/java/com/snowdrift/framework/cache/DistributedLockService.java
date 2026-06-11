package com.snowdrift.framework.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务接口
 * <p>
 * 提供分布式锁的编程式调用能力，由 Redisson 等后端实现。
 * 轻量场景推荐使用 {@link com.snowdrift.framework.cache.annotation.DistributedLock @DistributedLock} 注解。
 * </p>
 *
 * @author 83674
 * @date 2026/6/2
 * @since 1.0.0
 */
public interface DistributedLockService {

    /**
     * 尝试获取锁
     *
     * @param key       锁的 key
     * @param waitTime  获取锁的等待时间，0 表示不等待（立即返回）
     * @param leaseTime 持有锁的时间，-1 表示使用看门狗自动续期
     * @param unit      时间单位
     * @return true=获取成功
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁
     * <p>
     * 仅释放当前线程持有的锁
     * </p>
     *
     * @param key 锁的 key
     */
    void unlock(String key);

    /**
     * 判断是否被锁定
     *
     * @param key 锁的 key
     * @return true=已被锁定
     */
    boolean isLocked(String key);

    /**
     * 强制释放锁（管理员操作，无视持有线程）
     *
     * @param key 锁的 key
     */
    void forceUnlock(String key);

    /**
     * 带锁执行业务逻辑，自动加锁/解锁
     *
     * @param key       锁的 key
     * @param waitTime  获取锁的等待时间
     * @param leaseTime 持有锁的时间，-1 表示使用看门狗自动续期
     * @param unit      时间单位
     * @param supplier  业务逻辑
     * @param <T>       返回值类型
     * @return 业务执行结果
     * @throws com.snowdrift.framework.common.exception.BizException 获取锁失败时抛出
     */
    <T> T executeWithLock(String key, long waitTime, long leaseTime,
                          TimeUnit unit, Supplier<T> supplier);
}
