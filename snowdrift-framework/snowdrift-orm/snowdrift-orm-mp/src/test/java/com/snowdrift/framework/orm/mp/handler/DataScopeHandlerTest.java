package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.core.anno.DataScope;
import com.snowdrift.framework.orm.core.scope.DataScopeEnum;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataScopeHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 数据权限行级处理器黑盒测试：五种范围 SQL 生成、别名匹配、fail-closed 空集、重载冲突、_mpCount 与类加载失败
 * @since 1.0.0
 */
class DataScopeHandlerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    // ========== 测试用 Mapper（通过语句 ID 反查注解） ==========

    interface ScopedMapper {
        @DataScope(deptColumn = "dept_id", userColumn = "user_id")
        void selectScoped();

        @DataScope(alias = "o", deptColumn = "dept_id", userColumn = "user_id")
        void selectAliased();

        void selectPlain();
    }

    @DataScope(deptColumn = "d", userColumn = "u")
    interface ClassScopedMapper {
        void selectByClass();
    }

    interface OverloadMapper {
        @DataScope(deptColumn = "a")
        void select(String p);

        @DataScope(deptColumn = "b")
        void select(Integer p);
    }

    /** 可编程的假数据权限提供者 */
    static class FakeProvider implements IDataScopeProvider {
        DataScopeEnum scope = DataScopeEnum.ALL;
        List<Long> childDeptIds = List.of();
        List<Long> customDeptIds = List.of();

        @Override
        public DataScopeEnum getDataScope(Long userId) {
            return scope;
        }

        @Override
        public List<Long> getCustomDeptIds(Long userId) {
            return customDeptIds;
        }

        @Override
        public List<Long> getChildDeptIds(Long deptId, boolean inclusive) {
            return childDeptIds;
        }
    }

    private static String id(Class<?> mapper, String method) {
        return mapper.getName() + "." + method;
    }

    private static Table table(String name, String alias) {
        Table t = new Table(name);
        if (alias != null) {
            t.setAlias(new Alias(alias));
        }
        return t;
    }

    private static String segment(DataScopeHandler handler, Table table, String msId) {
        Expression expr = handler.getSqlSegment(table, null, msId);
        return expr == null ? null : expr.toString();
    }

    private static void login(Long userId, Long deptId) {
        SecurityContextHolder.setContext(SecurityContext.builder().userId(userId).deptId(deptId).build());
    }

    // ========== 无注解 / 解析 ==========

    @Test
    @DisplayName("语句无 @DataScope 时返回 null（不注入过滤）")
    void noAnnotationReturnsNull() {
        DataScopeHandler handler = new DataScopeHandler(new FakeProvider());

        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectPlain"))).isNull();
    }

    @Test
    @DisplayName("语句 ID 无法加载对应类（如纯 XML 命名空间）时返回 null，不报错")
    void unloadableClassReturnsNull() {
        DataScopeHandler handler = new DataScopeHandler(new FakeProvider());

        assertThat(segment(handler, table("orders", null), "com.nonexistent.NoSuchMapper.select")).isNull();
    }

    @Test
    @DisplayName("语句 ID 不含点（非法）时返回 null")
    void malformedStatementIdReturnsNull() {
        DataScopeHandler handler = new DataScopeHandler(new FakeProvider());

        assertThat(segment(handler, table("orders", null), "no_dot_id")).isNull();
    }

    @Test
    @DisplayName("_mpCount 分页计数语句后缀被剥离后仍能解析到注解")
    void stripsMpCountSuffix() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        login(1L, 5L);
        DataScopeHandler handler = new DataScopeHandler(provider);

        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped") + "_mpCount"))
                .isEqualTo("dept_id = 5");
    }

    @Test
    @DisplayName("方法无注解时回退到类级 @DataScope")
    void fallsBackToClassAnnotation() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        login(1L, 8L);
        DataScopeHandler handler = new DataScopeHandler(provider);

        assertThat(segment(handler, table("orders", null), id(ClassScopedMapper.class, "selectByClass")))
                .isEqualTo("d = 8");
    }

    @Test
    @DisplayName("同名重载挂载不同 @DataScope 时显式抛错，避免随机命中")
    void conflictingOverloadsThrow() {
        DataScopeHandler handler = new DataScopeHandler(new FakeProvider());
        login(1L, 5L);

        assertThatThrownBy(() -> segment(handler, table("orders", null), id(OverloadMapper.class, "select")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("多个不同的 @DataScope 重载");
    }

    // ========== provider 缺失 / 上下文缺失 ==========

    @Test
    @DisplayName("声明 @DataScope 却未装配 IDataScopeProvider 时抛错（安全控制不静默失效）")
    void missingProviderThrows() {
        DataScopeHandler handler = new DataScopeHandler(null);

        assertThatThrownBy(() -> segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置 IDataScopeProvider");
    }

    @Test
    @DisplayName("无登录上下文时返回 1=0 空集（fail-closed）")
    void noContextReturnsNoAccess() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        DataScopeHandler handler = new DataScopeHandler(provider);

        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    @Test
    @DisplayName("provider 返回 null 范围时按无权限 1=0 处理，不空指针")
    void nullScopeReturnsNoAccess() {
        FakeProvider provider = new FakeProvider();
        provider.scope = null;
        login(1L, 5L);
        DataScopeHandler handler = new DataScopeHandler(provider);

        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    // ========== 五种范围 ==========

    @Test
    @DisplayName("ALL：不注入任何过滤条件")
    void allScopeNoFilter() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.ALL;
        login(1L, 5L);

        assertThat(segment(new DataScopeHandler(provider), table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isNull();
    }

    @Test
    @DisplayName("DEPT：dept_id = 当前部门；缺 deptId 则 1=0")
    void deptScope() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        DataScopeHandler handler = new DataScopeHandler(provider);

        login(1L, 5L);
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("dept_id = 5");

        SecurityContextHolder.clear();
        login(1L, null);
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    @Test
    @DisplayName("SELF：user_id = 当前用户；缺 userId 则 1=0")
    void selfScope() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.SELF;
        DataScopeHandler handler = new DataScopeHandler(provider);

        login(9L, 5L);
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("user_id = 9");

        SecurityContextHolder.clear();
        login(null, 5L);
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    @Test
    @DisplayName("DEPT_AND_SUB：dept_id IN (子部门列表)；列表为空则 1=0")
    void deptAndSubScope() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT_AND_SUB;
        DataScopeHandler handler = new DataScopeHandler(provider);
        login(1L, 5L);

        provider.childDeptIds = List.of(1L, 2L, 3L);
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("dept_id IN (1, 2, 3)");

        provider.childDeptIds = List.of();
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    @Test
    @DisplayName("CUSTOM：dept_id IN (自定义部门列表)")
    void customScope() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.CUSTOM;
        provider.customDeptIds = List.of(7L);
        login(9L, 5L);

        assertThat(segment(new DataScopeHandler(provider), table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("dept_id IN (7)");
    }

    @Test
    @DisplayName("NONE：按无权限 1=0 处理")
    void noneScope() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.NONE;
        login(1L, 5L);

        assertThat(segment(new DataScopeHandler(provider), table("orders", null), id(ScopedMapper.class, "selectScoped")))
                .isEqualTo("1 = 0");
    }

    // ========== 别名匹配（多表 JOIN 防污染） ==========

    @Test
    @DisplayName("指定 alias 时仅对匹配别名的表注入，且列名带别名前缀")
    void aliasMatchingInjectsPrefixedColumn() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        login(1L, 5L);
        DataScopeHandler handler = new DataScopeHandler(provider);

        assertThat(segment(handler, table("orders", "o"), id(ScopedMapper.class, "selectAliased")))
                .isEqualTo("o.dept_id = 5");
    }

    @Test
    @DisplayName("指定 alias 时非目标表（别名不匹配）不注入，防止跨表污染")
    void aliasMismatchSkips() {
        FakeProvider provider = new FakeProvider();
        provider.scope = DataScopeEnum.DEPT;
        login(1L, 5L);
        DataScopeHandler handler = new DataScopeHandler(provider);

        // 别名不同
        assertThat(segment(handler, table("orders", "x"), id(ScopedMapper.class, "selectAliased"))).isNull();
        // 无别名时以表名比较，也不匹配 "o"
        assertThat(segment(handler, table("orders", null), id(ScopedMapper.class, "selectAliased"))).isNull();
    }
}
