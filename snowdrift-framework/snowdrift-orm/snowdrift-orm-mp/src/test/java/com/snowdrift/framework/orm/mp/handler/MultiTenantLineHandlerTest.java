package com.snowdrift.framework.orm.mp.handler;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MultiTenantLineHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 多租户行级处理器黑盒测试：租户表达式生成、无上下文 fail-closed、列名与忽略表大小写匹配
 * @since 1.0.0
 */
class MultiTenantLineHandlerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private MultiTenantLineHandler handler(OrmMpTenantProperties props) {
        return new MultiTenantLineHandler(props);
    }

    @Test
    @DisplayName("有租户上下文时返回租户ID的 LongValue 表达式")
    void returnsTenantIdExpression() {
        SecurityContextHolder.setContext(SecurityContext.builder().tenantId(5L).build());

        assertThat(handler(new OrmMpTenantProperties()).getTenantId().toString()).isEqualTo("5");
    }

    @Test
    @DisplayName("有上下文但租户ID为空时抛 BizException（fail-closed）")
    void throwsWhenTenantIdNull() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("u").build());

        assertThatThrownBy(() -> handler(new OrmMpTenantProperties()).getTenantId())
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未获取到租户上下文信息");
    }

    @Test
    @DisplayName("无安全上下文时抛 BizException（fail-closed，不跨租户读写）")
    void throwsWhenNoContext() {
        assertThatThrownBy(() -> handler(new OrmMpTenantProperties()).getTenantId())
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("租户列名取配置值，默认 tenant_id")
    void tenantIdColumn() {
        assertThat(handler(new OrmMpTenantProperties()).getTenantIdColumn()).isEqualTo("tenant_id");

        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setTenantIdColumn("org_id");
        assertThat(handler(props).getTenantIdColumn()).isEqualTo("org_id");
    }

    @Test
    @DisplayName("ignoreTable：命中忽略表（大小写不敏感）返回 true")
    void ignoreTableMatchesCaseInsensitive() {
        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setIgnoreTables(Set.of("sys_config"));

        assertThat(handler(props).ignoreTable("sys_config")).isTrue();
        assertThat(handler(props).ignoreTable("SYS_CONFIG")).isTrue();
        assertThat(handler(props).ignoreTable("sys_user")).isFalse();
    }

    @Test
    @DisplayName("边界：忽略表未配置/为空或表名空白时返回 false")
    void ignoreTableEdgeCases() {
        OrmMpTenantProperties nullSet = new OrmMpTenantProperties();
        nullSet.setIgnoreTables(null);
        assertThat(handler(nullSet).ignoreTable("any")).isFalse();

        OrmMpTenantProperties emptySet = new OrmMpTenantProperties();
        emptySet.setIgnoreTables(Set.of());
        assertThat(handler(emptySet).ignoreTable("any")).isFalse();

        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setIgnoreTables(Set.of("sys_config"));
        assertThat(handler(props).ignoreTable("  ")).isFalse();
        assertThat(handler(props).ignoreTable(null)).isFalse();
    }

    @Test
    @DisplayName("健壮性：忽略表集合含 null 元素时不抛 NPE，仍正确匹配")
    void ignoreTableToleratesNullElement() {
        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setIgnoreTables(new HashSet<>(Arrays.asList("sys_dict", null)));

        assertThat(handler(props).ignoreTable("sys_dict")).isTrue();
        assertThat(handler(props).ignoreTable("other")).isFalse();
    }
}
