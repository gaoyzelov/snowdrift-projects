package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.enums.DataScopeEnum;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.core.anno.DataScope;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataScopeHandler
 *
 * @author gaoyzelov
 * @date 2026/7/2
 * @description 数据权限 SQL 行级处理器
 * @since 1.0.0
 */
@Slf4j
public class DataScopeHandler implements MultiDataPermissionHandler {

    /**
     * 数据权限提供者；可为 {@code null}（未装配该 Bean 时，携带 {@code @DataScope} 的语句将显式抛错）
     */
    private final IDataScopeProvider provider;

    private final Map<String, DataScope> annotationCache = new ConcurrentHashMap<>();

    /**
     * 构造数据权限处理器
     * <p>provider 可为 {@code null}：此时若语句实际携带 {@code @DataScope}，
     * {@link #getSqlSegment} 将抛出 {@link BizException} 提示未配置 {@link IDataScopeProvider}，
     * 避免安全控制静默失效；未携带 {@code @DataScope} 的语句不受影响。</p>
     *
     * @param provider 数据权限提供者（业务应用实现，用于查询子部门列表和自定义部门列表；可为 null）
     */
    public DataScopeHandler(IDataScopeProvider provider) {
        this.provider = provider;
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScope scope = annotationCache.computeIfAbsent(mappedStatementId, this::resolveAnnotation);
        if (scope == null) {
            return null;
        }
        // 多表 JOIN 时，仅对与注解 alias 匹配的表注入条件，防止跨表污染
        if (!isTargetTable(table, scope)) {
            return null;
        }
        if (provider == null) {
            // 语句声明了 @DataScope 却未装配 IDataScopeProvider：静默不过滤会使安全控制失效，显式报错
            throw new BizException("使用 @DataScope 但未配置 IDataScopeProvider");
        }
        SecurityContext context;
        try {
            context = SecurityContextHolder.getContext();
        } catch (BizException e) {
            // 无登录上下文：按“无权限”处理（1=0，不返回任何行）
            return noAccess();
        }
        DataScopeEnum dataScope = provider.getDataScope(context.getUserId());
        if (dataScope == null) {
            // 提供方返回 null：按 NONE（无权限，fail-closed）处理为 1=0 空集，避免 switch 空指针
            log.debug("数据权限提供方返回 null，userId={}，按无权限（1=0）处理", context.getUserId());
            return noAccess();
        }
        if (dataScope == DataScopeEnum.ALL) {
            log.debug("数据权限类型为 {}，跳过数据权限过滤", dataScope);
            return null;
        }

        return buildExpression(dataScope, context, scope);
    }

    /**
     * 判断当前表是否为数据权限的目标表
     * <p>
     * {@link com.baomidou.mybatisplus.extension.plugins.inner.BaseMultiTableInnerInterceptor}
     * 在多表 JOIN 场景下会对每张表调用 {@link #getSqlSegment}，
     * 此方法确保只对 {@link DataScope#alias()} 指定的表注入过滤条件。
     * </p>
     * <ul>
     *   <li>注解未指定 alias → 视为单表场景，所有表均匹配</li>
     *   <li>注解指定了 alias → 仅当表的别名或表名与 alias 匹配时返回 true</li>
     * </ul>
     *
     * @param table 当前 SQL 中的表对象
     * @param scope 数据权限注解
     * @return true 表示当前表需要注入数据权限条件
     */
    private boolean isTargetTable(Table table, DataScope scope) {
        String targetAlias = StringUtils.trimToNull(scope.alias());
        if (targetAlias == null) {
            // 未指定 alias，视为单表场景，所有表均匹配
            return true;
        }
        String tableAlias = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        return targetAlias.equalsIgnoreCase(tableAlias);
    }

    private DataScope resolveAnnotation(String mappedStatementId) {
        int lastDot = mappedStatementId.lastIndexOf(StrConst.DOT);
        if (lastDot <= 0) return null;

        String className = mappedStatementId.substring(0, lastDot);
        String methodName = mappedStatementId.substring(lastDot + 1);
        // 移除 MyBatis-Plus 分页插件自动生成的 _mpCount 后缀
        if (methodName.endsWith("_mpCount")) {
            methodName = methodName.substring(0, methodName.length() - 8);
        }

        try {
            Class<?> clazz = Class.forName(className);
            // 遍历所有方法匹配名称，优先级高于类注解
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName)) {
                    DataScope anno = m.getAnnotation(DataScope.class);
                    if (anno != null) return anno;
                }
            }
            // 如果方法上没有注解，则检查类上是否有注解
            return clazz.getAnnotation(DataScope.class);
        } catch (ClassNotFoundException e) {
            log.debug("无法加载 Mapper 类: {}", className);
            return null;
        }
    }

    private Expression buildExpression(DataScopeEnum dataScope, SecurityContext context, DataScope scope) {
        String alias = StringUtils.trimToNull(scope.alias());
        String deptColumn = buildColumnName(scope.deptColumn(), alias);
        String userColumn = buildColumnName(scope.userColumn(), alias);

        return switch (dataScope) {
            case DEPT -> context.getDeptId() != null
                    ? new EqualsTo(new Column(deptColumn), new LongValue(context.getDeptId()))
                    : noAccess();
            case SELF -> context.getUserId() != null
                    ? new EqualsTo(new Column(userColumn), new LongValue(context.getUserId()))
                    : noAccess();
            case DEPT_AND_SUB -> context.getDeptId() != null
                    ? buildInExpression(deptColumn, provider.getChildDeptIds(context.getDeptId(), true))
                    : noAccess();
            case CUSTOM -> context.getUserId() != null
                    ? buildInExpression(deptColumn, provider.getCustomDeptIds(context.getUserId()))
                    : noAccess();
            default -> noAccess();
        };
    }

    /**
     * 生成“无权限”表达式：{@code 1 = 0}，用于无登录上下文、缺少过滤字段或过滤列表为空时返回空集
     */
    private Expression noAccess() {
        return new EqualsTo(new LongValue(1), new LongValue(0));
    }

    /**
     * 拼接带表别名前缀的列名
     * <p>
     * 当 {@code tableAlias} 不为空时返回 {@code alias.column}，否则返回裸列名。
     * </p>
     *
     * @param columnName 列名
     * @param tableAlias 表别名（可为 null）
     * @return 完整的列引用
     */
    private String buildColumnName(String columnName, String tableAlias) {
        if (StringUtils.isBlank(tableAlias)) {
            return columnName;
        }
        return tableAlias + StrConst.DOT + columnName;
    }

    /**
     * 构建 IN 表达式：{@code column IN (id1, id2, ...)}
     * <p>列表为空时返回 {@code 1=0}（空集），避免拼出非法的 {@code IN ()}。</p>
     *
     * @param column  列引用（可含表别名前缀）
     * @param deptIds 部门ID列表
     * @return IN 表达式；列表为空时返回 {@code 1=0}
     */
    private Expression buildInExpression(String column, List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return noAccess();
        }
        ParenthesedExpressionList<LongValue> expressionList = new ParenthesedExpressionList<>(
                deptIds.stream().map(LongValue::new).toList()
        );
        return new InExpression(new Column(column), expressionList);
    }
}
