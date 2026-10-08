package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.orm.core.scope.DataScopeEnum;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * 注解解析结果缓存
     * <p>值用 {@link Optional} 包装：{@code Optional.empty()} 表示"已确认该语句没有 {@code @DataScope}"，
     * 与"尚未解析"区分开。{@link ConcurrentHashMap#computeIfAbsent} 不会保存 {@code null}，
     * 若直接缓存 {@code null}，则每条无注解的语句在每次执行、每张表上都会重做一次
     * {@code Class.forName} + {@code getMethods()} 反射。</p>
     */
    private final Map<String, Optional<DataScope>> annotationCache = new ConcurrentHashMap<>();

    /**
     * 构造数据权限处理器
     * <p>provider 可为 {@code null}：此时若语句实际携带 {@code @DataScope}，
     * {@link #getSqlSegment} 将抛出 {@link BizException} 提示未配置 {@link IDataScopeProvider}，
     * 避免安全控制静默失效；未携带 {@code @DataScope} 的语句不受影响。</p>
     * <p>注解解析需从语句 ID 反推 Mapper 类。若该类加载失败（纯 XML mapper 的命名空间、
     * 类加载器隔离、shade/relocate 等），注解不会被解析，此时只能输出 WARN 日志，
     * 详见 {@link #resolveAnnotation(String)}。</p>
     *
     * @param provider 数据权限提供者（业务应用实现，用于查询子部门列表和自定义部门列表；可为 null）
     */
    public DataScopeHandler(IDataScopeProvider provider) {
        this.provider = provider;
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataScope scope = annotationCache
                .computeIfAbsent(mappedStatementId, id -> Optional.ofNullable(resolveAnnotation(id)))
                .orElse(null);
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
        String rawMethodName = mappedStatementId.substring(lastDot + 1);
        // 移除 MyBatis-Plus 分页插件自动生成的 _mpCount 后缀。
        // 用新变量承接而非重新赋值，使方法名保持 effectively final，可被下方 lambda 捕获。
        String methodName = rawMethodName.endsWith("_mpCount")
                ? rawMethodName.substring(0, rawMethodName.length() - 8)
                : rawMethodName;

        try {
            Class<?> clazz = Class.forName(className);
            // 同名重载可能挂载不同的 @DataScope：收集全部结果使其确定化，
            // 避免依赖 getMethods() 的返回顺序（JVM 不保证顺序）而随机命中某一个重载。
            List<DataScope> found = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .map(m -> m.getAnnotation(DataScope.class))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (found.size() == 1) {
                return found.get(0);
            }
            if (found.size() > 1) {
                throw new BizException("Mapper 方法存在多个不同的 @DataScope 重载，无法确定数据权限: "
                        + className + "#" + methodName);
            }
            // 方法上没有注解时，回退到类注解
            return clazz.getAnnotation(DataScope.class);
        } catch (ClassNotFoundException e) {
            // namespace 不是可加载的类时（典型：纯 XML mapper 的命名空间），本就无法声明 @DataScope，
            // 因此不注入谓词是正确结果，不报错以免误伤这类 mapper。
            // 但若命名空间本该是类却加载失败（类加载器隔离、shade/relocate、重命名等），
            // 注解意图会被静默丢弃 —— 故提升为 WARN，让"配置了却没生效"能够被发现。
            log.warn("无法加载 Mapper 类，数据权限注解不会被解析；若该语句本应受数据权限约束，"
                    + "请检查 MyBatis 命名空间与类加载器: {}", className);
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
