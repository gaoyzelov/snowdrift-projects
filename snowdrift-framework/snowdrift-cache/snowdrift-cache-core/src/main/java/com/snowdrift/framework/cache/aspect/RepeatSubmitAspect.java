package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.annotation.RepeatSubmit;
import com.snowdrift.framework.cache.util.SpELUtil;
import com.snowdrift.framework.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.time.Duration;

/**
 * 重复提交防护 AOP 切面
 * <p>
 * 拦截 {@link RepeatSubmit @RepeatSubmit} 注解的方法，
 * 在执行前检查是否重复提交，若已存在则拒绝，否则写入标记并放行。
 * </p>
 * <p>
 * <b>注意：</b>幂等标记的 TTL 应大于业务方法的最长执行时间。
 * 若业务执行时间超过 {@code interval}，标记会在处理过程中过期，
 * 导致并发请求穿透幂等防护。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(1)
public class RepeatSubmitAspect {

    private final ICacheService cacheService;

    public RepeatSubmitAspect(ICacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        String key = SpELUtil.parseExpression(repeatSubmit.key(), joinPoint);
        Duration ttl = Duration.of(repeatSubmit.interval(), repeatSubmit.timeUnit().toChronoUnit());

        log.debug("重复提交检查: key={}, interval={}", key, ttl);

        // 尝试写入标记，写入失败表示已存在（重复提交）
        boolean success = cacheService.putIfAbsent(key, "1", ttl);
        if (!success) {
            log.warn("检测到重复提交: key={}", key);
            throw new BizException(repeatSubmit.message(), repeatSubmit.args());
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 业务异常时删除标记，允许用户修正后重新提交
            cacheService.delete(key);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw e;
        }
    }
}
