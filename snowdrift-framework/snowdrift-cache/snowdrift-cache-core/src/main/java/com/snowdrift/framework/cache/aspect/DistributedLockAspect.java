package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.cache.DistributedLockService;
import com.snowdrift.framework.cache.annotation.DistributedLock;
import com.snowdrift.framework.cache.util.SpelUtil;
import com.snowdrift.framework.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 AOP 切面
 * <p>
 * 拦截 {@link DistributedLock @DistributedLock} 注解的方法，
 * 自动进行加锁 → 执行业务 → 释放锁。
 * 仅在容器中存在 {@link DistributedLockService} Bean 时生效（即 Redisson 模块激活时）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(2)
public class DistributedLockAspect {

    private final DistributedLockService lockService;

    public DistributedLockAspect(DistributedLockService lockService) {
        this.lockService = lockService;
    }

    /**
     * 环绕通知：加锁 → 执行 → 解锁
     */
    @Around("@annotation(lockAnno)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock lockAnno) throws Throwable {
        String key = SpelUtil.parseExpression(lockAnno.key(), joinPoint);
        long waitTime = lockAnno.waitTime();
        long leaseTime = lockAnno.leaseTime();
        TimeUnit timeUnit = lockAnno.timeUnit();

        log.debug("尝试获取分布式锁: key={}, waitTime={}{}", key, waitTime, timeUnit);

        boolean locked = lockService.tryLock(key, waitTime, leaseTime, timeUnit);
        if (!locked) {
            log.warn("获取分布式锁失败: key={}", key);
            // message 为 i18n key，由全局 WebExceptionHandler 统一解析
            throw new BizException(lockAnno.message(), lockAnno.args());
        }

        try {
            log.debug("获取分布式锁成功，执行业务: key={}", key);
            return joinPoint.proceed();
        } finally {
            lockService.unlock(key);
            log.debug("释放分布式锁: key={}", key);
        }
    }
}
