package com.snowdrift.cache.redis.aspect;

import com.snowdrift.cache.redis.anno.DistributedLock;
import com.snowdrift.cache.redis.exception.LockException;
import com.snowdrift.cache.redis.service.IRedisService;
import com.snowdrift.core.utils.SpELUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DistributedLockAspect
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/8/13 14:50
 */
@Slf4j
@Aspect
public class DistributedLockAspect {

    @Resource
    private IRedisService redisService;

    @Around("@annotation(com.snowdrift.cache.redis.anno.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLock anno = method.getAnnotation(DistributedLock.class);
        String lockKey = anno.value();
        if (StringUtils.isNotBlank(lockKey) && lockKey.matches("^#.*.$")) {
            lockKey = parseLockKey(lockKey, joinPoint, method);
        }
        if (anno.block()) {
            // 阻塞获取锁
            redisService.lock(lockKey, anno.leaseTime());
        }else{
            // 尝试获取锁
            boolean locked = redisService.tryLock(lockKey, anno.leaseTime(), anno.waitTime());
            if (!locked) {
                throw new LockException(anno.failMsg());
            }
        }
        // 获取锁成功
        try {
            return joinPoint.proceed();
        } finally {
            //如果该线程还持有该锁，那么释放该锁。如果该线程不持有该锁，说明该线程的锁已到过期时间，自动释放锁
            if (redisService.isHeldByCurrentThread(lockKey)) {
                redisService.unlock(lockKey);
            }
        }
    }

    /**
     * 解析锁key
     * @param spELKey 锁key
     * @param joinPoint 切点
     * @param method 方法
     * @return 解析后的锁key
     */
    private String parseLockKey(String spELKey, ProceedingJoinPoint joinPoint,Method method) {
        Object[] args = joinPoint.getArgs();
        StandardReflectionParameterNameDiscoverer localVariableTable = new StandardReflectionParameterNameDiscoverer();
        String[] params = localVariableTable.getParameterNames(method);
        if (Objects.isNull(params) || params.length == 0) {
            return spELKey;
        }
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            variables.put(params[i], args[i]);
        }
        return SpELUtil.evaluate(spELKey, variables, String.class);
    }
}