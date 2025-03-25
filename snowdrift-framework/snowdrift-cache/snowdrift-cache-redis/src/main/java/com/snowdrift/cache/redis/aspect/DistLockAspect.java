package com.snowdrift.cache.redis.aspect;

import com.snowdrift.cache.redis.anno.DistLock;
import com.snowdrift.cache.redis.exception.DistLockException;
import com.snowdrift.cache.redis.service.IRedisLockService;
import com.snowdrift.core.constant.RegexConst;
import com.snowdrift.core.utils.SpELUtil;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DistLockAspect
 *
 * @author gaoye
 * @date 2025/03/20 09:45:07
 * @description 分布式锁切面
 * @since 1.0.0
 */
@Aspect
public class DistLockAspect {

    @Resource
    private IRedisLockService redisLockService;

    /**
     * 环绕通知
     *
     * @param joinPoint 切点
     * @param distLock  分布式锁注解
     * @return Object
     */
    @Around("@annotation(distLock)")
    public Object doAround(ProceedingJoinPoint joinPoint, DistLock distLock) throws Throwable {
        String lockKey = getLockKey(joinPoint, distLock);
        boolean locked;
        if (distLock.block()) {
            redisLockService.lock(lockKey);
            locked = true;
        } else {
            locked = redisLockService.tryLock(lockKey, distLock.lockTime(), distLock.waitTime());
        }
        if (!locked) {
            throw new DistLockException(distLock.msg());
        }
        try {
            // 加锁成功，执行目标方法
            return joinPoint.proceed();
        } finally {
            // 如果该线程还持有该锁，那么释放该锁。
            // 如果该线程不持有该锁，说明该线程的锁已到过期时间，自动释放锁
            if (redisLockService.isHeldByCurrentThread(lockKey)) {
                redisLockService.unlock(lockKey);
            }
        }
    }

    /**
     * 获取锁的key
     *
     * @param joinPoint 切点
     * @param distLock  分布式锁注解
     * @return String
     */
    private String getLockKey(ProceedingJoinPoint joinPoint, DistLock distLock) {
        String prefix = distLock.value();
        String key = distLock.key();
        if (StringUtils.isBlank(prefix)) {
            throw new DistLockException("分布式锁注解【value】不能为空");
        }
        if (StringUtils.isBlank(key) || !key.matches(RegexConst.SPEL)) {
            return prefix + key;
        }
        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        LocalVariableTableParameterNameDiscoverer localVariableTable = new LocalVariableTableParameterNameDiscoverer();
        String[] params = localVariableTable.getParameterNames(method);
        if (Objects.isNull(params) || params.length == 0) {
            return prefix + key;
        }
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            variables.put(params[i], args[i]);
        }
        return prefix + SpELUtil.evaluate(key, variables, String.class);
    }
}