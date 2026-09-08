package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.annotation.RepeatSubmit;
import com.snowdrift.framework.cache.util.SpELUtil;
import com.snowdrift.framework.base.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.time.Duration;
import java.util.UUID;

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
        if (StringUtils.isBlank(key)) {
            throw new BizException("SpEL 防重 key 解析失败，请检查 @RepeatSubmit key 配置: " + repeatSubmit.key());
        }
        Duration ttl = Duration.of(repeatSubmit.interval(), repeatSubmit.timeUnit().toChronoUnit());

        log.debug("重复提交检查: key={}, interval={}", key, ttl);

        // 写入本请求唯一 nonce 作为幂等标记值，用于失败清理时区分是否仍为本请求写入的标记
        String nonce = UUID.randomUUID().toString();
        // 尝试写入标记，写入失败表示已存在（重复提交）
        boolean success = cacheService.putIfAbsent(key, nonce, ttl);
        if (!success) {
            log.warn("检测到重复提交: key={}", key);
            throw new BizException(repeatSubmit.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 执行失败时删除标记，允许用户修正后重新提交；
            // 仅当缓存中仍为本请求写入的 nonce 时才删除，避免误删“本请求标记已过期、
            // 被并发请求重新写入”的新标记。读-删之间存在极小竞态窗口，
            // 但 ICacheService 未提供原子 compare-and-remove 原语，此处接受该残余竞态；
            // 删除失败仅记录日志，不覆盖原始业务异常
            try {
                String current = cacheService.get(key, String.class);
                if (nonce.equals(current)) {
                    cacheService.delete(key);
                }
            } catch (Exception deleteEx) {
                log.error("重复提交标记删除失败: key={}", key, deleteEx);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw e;
        }
    }
}
