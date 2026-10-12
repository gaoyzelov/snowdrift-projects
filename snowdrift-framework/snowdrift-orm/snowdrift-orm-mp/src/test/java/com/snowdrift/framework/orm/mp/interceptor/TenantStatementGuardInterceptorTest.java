package com.snowdrift.framework.orm.mp.interceptor;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantStatementGuardInterceptorTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 多租户语句守卫测试：MyBatis-Plus 无法注入租户条件的语句必须被拒绝，而不是静默放行
 * @since 1.0.0
 */
class TenantStatementGuardInterceptorTest {

    private TenantStatementGuardInterceptor guard(Set<String> ignoreTables) {
        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setEnabled(Boolean.TRUE);
        props.setIgnoreTables(ignoreTables);
        return new TenantStatementGuardInterceptor(new MultiTenantLineHandler(props));
    }

    @Test
    @DisplayName("REPLACE 命中租户表时被拒绝：MyBatis-Plus 不为 Upsert 注入租户条件")
    void rejectsReplaceOnTenantTable() {
        assertThatThrownBy(() -> guard(Set.of()).checkSql("REPLACE INTO t_order (id, amount) VALUES (1, 100)"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("REPLACE");
    }

    @Test
    @DisplayName("REPLACE ... SELECT 被拒绝：作为数据源的 SELECT 同样不带租户条件")
    void rejectsReplaceSelectOnTenantTable() {
        assertThatThrownBy(() -> guard(Set.of())
                .checkSql("REPLACE INTO t_order (id, amount) SELECT id, amount FROM t_secret"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("MERGE 命中租户表时被拒绝")
    void rejectsMergeOnTenantTable() {
        assertThatThrownBy(() -> guard(Set.of()).checkSql(
                "MERGE INTO t_order o USING t_secret t ON (o.id = t.id) WHEN MATCHED THEN UPDATE SET o.amount = t.amount"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("MERGE");
    }

    @Test
    @DisplayName("语句只涉及被忽略的表时放行（全局共享表不受租户隔离约束）")
    void allowsUnsupportedStatementOnIgnoredTables() {
        assertThatCode(() -> guard(Set.of("sys_config"))
                .checkSql("REPLACE INTO sys_config (id, value) VALUES (1, 'x')"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MyBatis-Plus 能处理的四类语句一律不被拦截")
    void allowsSupportedStatements() {
        TenantStatementGuardInterceptor guard = guard(Set.of());
        assertThatCode(() -> guard.checkSql("SELECT * FROM t_order WHERE id = 1")).doesNotThrowAnyException();
        assertThatCode(() -> guard.checkSql("INSERT INTO t_order (id) VALUES (1)")).doesNotThrowAnyException();
        assertThatCode(() -> guard.checkSql("UPDATE t_order SET amount = 1 WHERE id = 1")).doesNotThrowAnyException();
        assertThatCode(() -> guard.checkSql("DELETE FROM t_order WHERE id = 1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CTE（WITH ... SELECT）属于可处理的 SELECT，不被拦截")
    void allowsCteSelect() {
        assertThatCode(() -> guard(Set.of())
                .checkSql("WITH t AS (SELECT id FROM t_order) SELECT * FROM t"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("空 SQL 不拦截（交给 MyBatis 自身处理）")
    void allowsBlankSql() {
        assertThatCode(() -> guard(Set.of()).checkSql("  ")).doesNotThrowAnyException();
    }
}
