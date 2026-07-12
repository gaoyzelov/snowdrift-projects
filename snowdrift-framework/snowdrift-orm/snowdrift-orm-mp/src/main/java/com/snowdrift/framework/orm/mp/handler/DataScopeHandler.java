package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.enums.DataScopeEnum;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.core.anno.DataScope;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.collections4.CollectionUtils;
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
     * 数据权限提供者，可为 null。
     * 为 null 时 DEPT_AND_SUB 降级为 DEPT，CUSTOM 降级为SELF。
     */
    private final IDataScopeProvider dataScopeProvider;

    private final Map<String, DataScope> annotationCache = new ConcurrentHashMap<>();

    /**
     * 无 provider 构造 — DEPT_AND_SUB 降级为 DEPT，CUSTOM 降级为 SELF
     */
    public DataScopeHandler() {
        this.dataScopeProvider = null;
    }

    /**
     * 带 provider 构造 — 支持 DEPT_AND_SUB 和 CUSTOM 实时查询
     *
     * @param dataScopeProvider 数据权限提供者（业务应用实现，用于查询子部门列表和自定义部门列表）
     */
    public DataScopeHandler(IDataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
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
        SecurityContext context = SecurityContextHolder.getRequiredContext();
        DataScopeEnum dataScope = DataScopeEnum.of(context.getDataScope());
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
            methodName =  methodName.substring(0, methodName.length() - 8);
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
            case ALL -> null;
            case DEPT -> {
                Long deptId = context.getDeptId();
                if (deptId == null) {
                    log.debug("DEPT 模式下 deptId 为空，跳过数据权限过滤");
                    yield null;
                }
                yield new EqualsTo(new Column(deptColumn), new LongValue(deptId));
            }
            case SELF -> {
                Long userId = context.getUserId();
                if (userId == null) {
                    log.debug("SELF 模式下 userId 为空，跳过数据权限过滤");
                    yield null;
                }
                yield new EqualsTo(new Column(userColumn), new LongValue(userId));
            }
            case DEPT_AND_SUB -> {
                Long deptId = context.getDeptId();
                if (deptId == null) {
                    yield null;
                }
                if (dataScopeProvider == null) {
                    // 无 provider 时降级为 DEPT 模式
                    log.debug("DEPT_AND_SUB 模式无 IDataScopeProvider，降级为 DEPT");

                    yield new EqualsTo(new Column(deptColumn), new LongValue(deptId));
                }
                List<Long> childDeptIds = dataScopeProvider.getChildDeptIds(context.getDeptId());
                if (CollectionUtils.isEmpty(childDeptIds)) {
                    log.debug("DEPT_AND_SUB 模式下 childDeptIds 为空，跳过数据权限过滤");
                    yield null;
                }
                yield buildInExpression(deptColumn, childDeptIds);
            }
            case CUSTOM -> {
                Long userId = context.getUserId();
                if (userId == null) {
                    yield null;
                }
                if (dataScopeProvider == null) {
                    // 无 provider 降级为 SELF
                    log.warn("CUSTOM 模式无 IDataScopeProvider，降级为 SELF");
                    yield new EqualsTo(new Column(userColumn), new LongValue(userId));
                }
                List<Long> customDeptIds = dataScopeProvider.getCustomDeptIds(userId);
                if (CollectionUtils.isEmpty(customDeptIds)) {
                    log.debug("CUSTOM 模式下 customDeptIds 为空，跳过数据权限过滤");
                    yield null;
                }
                yield buildInExpression(deptColumn, customDeptIds);
            }
        };
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
     *
     * @param column  列引用（可含表别名前缀）
     * @param deptIds 部门ID列表（调用前需确保非空）
     * @return IN 表达式
     */
    private Expression buildInExpression(String column, List<Long> deptIds) {
        ExpressionList<LongValue> expressionList = new ExpressionList<>(
                deptIds.stream().map(LongValue::new).toList()
        );
        return new InExpression(new Column(column), expressionList);
    }
}
