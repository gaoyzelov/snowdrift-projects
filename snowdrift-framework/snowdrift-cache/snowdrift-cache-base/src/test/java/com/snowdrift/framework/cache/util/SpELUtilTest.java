package com.snowdrift.framework.cache.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpELUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description SpEL 解析工具类单元测试
 * @since 1.0.0
 */
class SpELUtilTest {

    @SuppressWarnings("unused")
    static class Target {
        public void withArgs(Long orderId, String type) {
        }

        public void noArgs() {
        }
    }

    private static ProceedingJoinPoint joinPoint(String methodName, Class<?>[] types, Object[] args) throws Exception {
        Method method = Target.class.getMethod(methodName, types);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    @Test
    @DisplayName("不含 # 的表达式按静态 key 原样返回（不做 SpEL 解析）")
    void staticKeyPassesThrough() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{1L, "vip"});

        assertThat(SpELUtil.parseExpression("order:pay", joinPoint)).isEqualTo("order:pay");
    }

    @Test
    @DisplayName("含 # 时按方法参数求值")
    void resolvesFromArgs() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{123L, "vip"});

        assertThat(SpELUtil.parseExpression("#orderId", joinPoint)).isEqualTo("123");
        assertThat(SpELUtil.parseExpression("'order:pay:' + #orderId", joinPoint)).isEqualTo("order:pay:123");
        assertThat(SpELUtil.parseExpression("#type", joinPoint)).isEqualTo("vip");
    }

    @Test
    @DisplayName("求值结果为 null 时返回 null，由调用方快速失败")
    void nullResultReturnsNull() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{1L, null});

        assertThat(SpELUtil.parseExpression("#type", joinPoint)).isNull();
    }

    @Test
    @DisplayName("表达式非法或参数名不存在时返回 null 而不是抛异常")
    void invalidExpressionReturnsNull() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{1L, "vip"});

        assertThat(SpELUtil.parseExpression("#notExists", joinPoint)).isNull();
        assertThat(SpELUtil.parseExpression("#orderId +", joinPoint)).isNull();
        assertThat(SpELUtil.parseExpression("#orderId.invalidCall()", joinPoint)).isNull();
    }

    @Test
    @DisplayName("无参方法的表达式求值为 null")
    void noArgs() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("noArgs", new Class<?>[0], new Object[0]);

        assertThat(SpELUtil.parseExpression("#orderId", joinPoint)).isNull();
        // 不含 # 的表达式走“静态 key 原样返回”分支：连单引号一起返回，故静态 key 请直接写裸文本
        assertThat(SpELUtil.parseExpression("fixed:key", joinPoint)).isEqualTo("fixed:key");
        assertThat(SpELUtil.parseExpression("'fixed:key'", joinPoint)).isEqualTo("'fixed:key'");
    }

    @Test
    @DisplayName("同一表达式重复解析结果一致（内部缓存不得改变语义）")
    void cachedResolutionIsStable() throws Exception {
        ProceedingJoinPoint first = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{7L, "a"});
        ProceedingJoinPoint second = joinPoint("withArgs", new Class<?>[]{Long.class, String.class}, new Object[]{8L, "b"});

        assertThat(SpELUtil.parseExpression("'k:' + #orderId", first)).isEqualTo("k:7");
        assertThat(SpELUtil.parseExpression("'k:' + #orderId", second)).isEqualTo("k:8");
        assertThat(SpELUtil.parseExpression("'k:' + #orderId", first)).isEqualTo("k:7");
    }
}
