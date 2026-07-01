package com.snowdrift.framework.cache.util;

import com.snowdrift.framework.common.constant.StrConst;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 表达式解析工具类
 * <p>
 * 提供基于 AOP 方法参数的 SpEL 表达式解析能力，
 * 供 {@link com.snowdrift.framework.cache.aspect.DistributedLockAspect} 和
 * {@link com.snowdrift.framework.cache.aspect.RepeatSubmitAspect} 共用。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/11
 * @since 1.0.0
 */
@Slf4j
public final class SpelUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelUtil() {
    }

    /**
     * 解析 SpEL 表达式，从 AOP 方法参数中提取值
     * <p>
     * 如果表达式不包含 {@code #} 占位符则直接返回原文；
     * 解析失败时降级返回原始表达式。
     * </p>
     *
     * @param expression SpEL 表达式
     * @param joinPoint  AOP 切入点
     * @return 解析后的字符串值，解析失败返回原始表达式
     */
    public static String parseExpression(String expression, ProceedingJoinPoint joinPoint) {
        if (!expression.contains(StrConst.HASH)) {
            return expression;
        }

        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            EvaluationContext context = new StandardEvaluationContext();

            String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            Object value = PARSER.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : expression;
        } catch (Exception e) {
            log.warn("解析 SpEL 表达式失败，使用原始值: {}", expression, e);
            return expression;
        }
    }
}
