package com.snowdrift.framework.cache.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.snowdrift.framework.base.constant.StrConst;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

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
public final class SpELUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final Cache<String, Expression> EXPRESSION_CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    private SpELUtil() {
    }

    /**
     * 解析 SpEL 表达式，从 AOP 方法参数中提取值
     * <p>
     * 如果表达式不包含 {@code #} 占位符则直接返回原文（静态 key）；
     * 包含 {@code #} 时按 SpEL 解析，解析失败或求值结果为 null 时返回 {@code null}，
     * 由调用方将 null/空结果视为配置错误快速失败，避免 key 静默降级为全局锁 / 全局防重标记。
     * </p>
     *
     * @param expression SpEL 表达式
     * @param joinPoint  AOP 切入点
     * @return 解析后的字符串值；解析失败或求值结果为 null 时返回 null；无 {@code #} 的静态 key 原样返回
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

            Expression expr;
            try {
                expr = EXPRESSION_CACHE.get(expression, () -> PARSER.parseExpression(expression));
            } catch (ExecutionException e) {
                log.warn("从缓存获取 SpEL 表达式失败，降级直接解析: {}", expression, e);
                expr = PARSER.parseExpression(expression);
            }
            Object value = expr.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("解析 SpEL 表达式失败: {}", expression, e);
            return null;
        }
    }
}
