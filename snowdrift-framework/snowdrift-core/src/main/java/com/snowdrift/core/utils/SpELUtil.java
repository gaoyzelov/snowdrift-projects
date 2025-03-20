package com.snowdrift.core.utils;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * SpELUtil
 *
 * @author gaoye
 * @date 2025/03/20 09:57:58
 * @description SpEL解析工具类
 * @since 1.0.0
 */
public class SpELUtil {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private SpELUtil() {
    }

    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @return 解析后的结果
     */
    public static Object evaluate(String expression) {
        return evaluate(expression, Object.class);
    }


    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @param clazz      预期的结果类型
     * @param <T>        结果类型
     * @return 解析后的结果
     */
    public static <T> T evaluate(String expression, Class<T> clazz) {
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(clazz);
    }

    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @param root       根对象
     * @return 解析后的结果
     */
    public static Object evaluate(String expression, Object root) {
        return evaluate(expression, root, Object.class);
    }

    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @param root       根对象
     * @param clazz      预期的结果类型
     * @param <T>        结果类型
     * @return 解析后的结果
     */
    public static <T> T evaluate(String expression, Object root, Class<T> clazz) {
        Expression exp = parser.parseExpression(expression);
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        return exp.getValue(context, clazz);
    }

    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @param variables  变量映射
     * @return 解析后的结果
     */
    public static Object evaluate(String expression, Map<String, Object> variables) {
        return evaluate(expression, variables, Object.class);
    }

    /**
     * 解析 SpEL 表达式并返回结果
     *
     * @param expression 表达式字符串
     * @param variables  变量映射
     * @param clazz      预期的结果类型
     * @param <T>        结果类型
     * @return 解析后的结果
     */
    public static <T> T evaluate(String expression, Map<String, Object> variables, Class<T> clazz) {
        Expression exp = parser.parseExpression(expression);
        EvaluationContext context = new StandardEvaluationContext();
        if (variables != null && !variables.isEmpty()) {
            variables.forEach(context::setVariable);
        }
        return exp.getValue(context, clazz);
    }
}