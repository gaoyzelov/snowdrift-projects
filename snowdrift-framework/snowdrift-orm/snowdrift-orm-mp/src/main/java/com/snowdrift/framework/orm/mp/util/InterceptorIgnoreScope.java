package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MyBatis-Plus 拦截器忽略策略的作用域管理
 * <p>
 * {@link TenantUtil} 与 {@link DataScopeUtil} 的公共实现。存在的理由是 MP 自身不提供可组合的作用域原语：
 * </p>
 * <ul>
 *   <li>{@link InterceptorIgnoreHelper#handle(IgnoreStrategy)} 是<b>整体替换</b>（内部 {@code ThreadLocal.set}），
 *       不是合并；</li>
 *   <li>{@link InterceptorIgnoreHelper#clearIgnoreStrategy()} 是 {@code remove}，会清掉<b>整个</b>线程本地策略；</li>
 *   <li>{@link InterceptorIgnoreHelper#execute(IgnoreStrategy, Supplier)} 同样是 handle+clear，
 *       其 Javadoc 明确写着「结束必定会释放掉当前线程上的策略」「不要和 handle 混合使用」。</li>
 * </ul>
 * <p>
 * 直接用它们做嵌套会出两个问题：
 * </p>
 * <ol>
 *   <li><b>体内丢标记</b>：内层 {@code handle} 整体替换，外层刚设置的忽略标记在内层体内就已失效；</li>
 *   <li><b>退出丢标记</b>：内层 {@code clearIgnoreStrategy} 把外层的策略一并清掉。</li>
 * </ol>
 * <p>
 * 因此这里维护自己的栈：进入时把「外层已累积的标记」与「本次请求的标记」取并集后整体设置，
 * 退出时弹出并恢复栈顶（栈空则清理）。这样嵌套的调用在体内与退出后都保持正确。
 * </p>
 * <p>
 * <b>已知边界</b>：若使用者在进入本作用域<b>之前</b>自行调用过 {@code InterceptorIgnoreHelper.handle(...)}，
 * MP 没有提供读取线程本地策略的公开途径，无法快照，退出时仍会将其清理（与修复前行为一致，属 MP 的能力限制而非本类引入）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @since 1.0.0
 */
final class InterceptorIgnoreScope {

    /**
     * 当前线程上由本框架设置的忽略策略栈（栈顶为生效策略）。
     * 只记录「我们自己设置过的」，因此无法覆盖使用者直接调用 MP API 的情形。
     */
    private static final ThreadLocal<Deque<IgnoreStrategy>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private InterceptorIgnoreScope() {
    }

    /**
     * 在「当前策略 ∪ 本次请求策略」的上下文中执行 {@link Runnable}
     *
     * @param requested 本次请求的忽略标记
     * @param runnable  要执行的操作
     */
    static void run(IgnoreStrategy requested, Runnable runnable) {
        enter(requested);
        try {
            runnable.run();
        } finally {
            exit();
        }
    }

    /**
     * 在「当前策略 ∪ 本次请求策略」的上下文中执行 {@link Supplier}
     *
     * @param requested 本次请求的忽略标记
     * @param supplier  要执行的操作
     * @param <T>       返回值类型
     * @return 操作结果
     */
    static <T> T call(IgnoreStrategy requested, Supplier<T> supplier) {
        enter(requested);
        try {
            return supplier.get();
        } finally {
            exit();
        }
    }

    private static void enter(IgnoreStrategy requested) {
        Deque<IgnoreStrategy> stack = STACK.get();
        IgnoreStrategy effective = merge(stack.peek(), requested);
        stack.push(effective);
        InterceptorIgnoreHelper.handle(effective);
    }

    private static void exit() {
        Deque<IgnoreStrategy> stack = STACK.get();
        stack.pop();
        if (stack.isEmpty()) {
            // 栈空说明本框架的忽略已全部退出：清空，避免标记泄漏到同一线程的后续请求
            STACK.remove();
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        } else {
            InterceptorIgnoreHelper.handle(stack.peek());
        }
    }

    /**
     * 取两个策略的并集：任一为 true 即为 true；两者均为 null 则保持 null
     *
     * @param outer 外层已累积的策略，可为 null
     * @param inner 本次请求的策略，可为 null
     * @return 并集；两者皆空时返回 null
     */
    private static IgnoreStrategy merge(IgnoreStrategy outer, IgnoreStrategy inner) {
        if (outer == null) {
            return inner;
        }
        if (inner == null) {
            return outer;
        }
        return IgnoreStrategy.builder()
                .tenantLine(or(outer.getTenantLine(), inner.getTenantLine()))
                .dynamicTableName(or(outer.getDynamicTableName(), inner.getDynamicTableName()))
                .blockAttack(or(outer.getBlockAttack(), inner.getBlockAttack()))
                .illegalSql(or(outer.getIllegalSql(), inner.getIllegalSql()))
                .dataPermission(or(outer.getDataPermission(), inner.getDataPermission()))
                .others(mergeOthers(outer.getOthers(), inner.getOthers()))
                .build();
    }

    private static Boolean or(Boolean left, Boolean right) {
        if (Boolean.TRUE.equals(left) || Boolean.TRUE.equals(right)) {
            return Boolean.TRUE;
        }
        if (left == null && right == null) {
            return null;
        }
        return Boolean.FALSE;
    }

    private static Map<String, Boolean> mergeOthers(Map<String, Boolean> outer, Map<String, Boolean> inner) {
        if (outer == null || outer.isEmpty()) {
            return inner;
        }
        if (inner == null || inner.isEmpty()) {
            return outer;
        }
        Map<String, Boolean> merged = new HashMap<>(outer);
        merged.putAll(inner);
        return merged;
    }
}
