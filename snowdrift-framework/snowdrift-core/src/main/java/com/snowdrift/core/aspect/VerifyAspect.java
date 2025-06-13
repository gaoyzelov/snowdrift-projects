package com.snowdrift.core.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * VerifyAspect
 *
 * @author gaoye
 * @date 2025/06/13 14:58:41
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
@Aspect
public class VerifyAspect {

    @Before("@annotation(com.snowdrift.core.anno.Verify)")
    public void verify(JoinPoint joinPoint) {
        log.info("开始验证参数");
    }
}