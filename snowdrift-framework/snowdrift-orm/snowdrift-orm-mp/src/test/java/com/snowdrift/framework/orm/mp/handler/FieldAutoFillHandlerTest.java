package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.core.entity.BaseEntity;
import com.snowdrift.framework.orm.core.entity.TenantEntity;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FieldAutoFillHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 字段自动填充处理器黑盒测试：操作人解析（昵称>账号>system）、审计字段填充、不覆盖已有值、租户 fail-closed 与默认租户
 * @since 1.0.0
 */
class FieldAutoFillHandlerTest {

    @TableName("test_entity")
    static class TestEntity extends BaseEntity {
    }

    @TableName("test_tenant_entity")
    static class TestTenantEntity extends TenantEntity {
    }

    @BeforeAll
    static void initTableInfo() {
        initTableInfo(TestEntity.class);
        initTableInfo(TestTenantEntity.class);
    }

    private static void initTableInfo(Class<?> clazz) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, clazz);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    private FieldAutoFillHandler handler(boolean tenantEnabled, Long defaultTenantId) {
        OrmMpTenantProperties props = new OrmMpTenantProperties();
        props.setEnabled(tenantEnabled);
        props.setDefaultTenantId(defaultTenantId);
        return new FieldAutoFillHandler(props);
    }

    private MetaObject meta(Object entity) {
        return SystemMetaObject.forObject(entity);
    }

    @Test
    @DisplayName("INSERT：有昵称时操作人取昵称，审计时间被填充")
    void insertFillUsesNickname() {
        SecurityContextHolder.setContext(SecurityContext.builder().nickname("Nick").username("acct").build());
        TestEntity entity = new TestEntity();

        handler(false, null).insertFill(meta(entity));

        assertThat(entity.getCreateBy()).isEqualTo("Nick");
        assertThat(entity.getUpdateBy()).isEqualTo("Nick");
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("INSERT：无昵称时操作人取账号")
    void insertFillFallsBackToUsername() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("acct").build());
        TestEntity entity = new TestEntity();

        handler(false, null).insertFill(meta(entity));

        assertThat(entity.getCreateBy()).isEqualTo("acct");
    }

    @Test
    @DisplayName("INSERT：无登录上下文时操作人降级为 system，不抛异常")
    void insertFillAnonymousUsesSystem() {
        TestEntity entity = new TestEntity();

        handler(false, null).insertFill(meta(entity));

        assertThat(entity.getCreateBy()).isEqualTo("system");
        assertThat(entity.getUpdateBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("INSERT：字段已有值时不覆盖（strict fill 语义）")
    void insertFillDoesNotOverrideExisting() {
        SecurityContextHolder.setContext(SecurityContext.builder().nickname("Nick").build());
        TestEntity entity = new TestEntity();
        entity.setCreateBy("preset");

        handler(false, null).insertFill(meta(entity));

        assertThat(entity.getCreateBy()).isEqualTo("preset");
    }

    @Test
    @DisplayName("UPDATE：仅填充 updateBy/updateTime，不触碰 createBy/createTime")
    void updateFillOnlyTouchesUpdateFields() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("acct").build());
        TestEntity entity = new TestEntity();

        handler(false, null).updateFill(meta(entity));

        assertThat(entity.getUpdateBy()).isEqualTo("acct");
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getCreateBy()).isNull();
        assertThat(entity.getCreateTime()).isNull();
    }

    @Test
    @DisplayName("INSERT：多租户开启且有上下文时填充 tenantId")
    void insertFillSetsTenantFromContext() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("acct").tenantId(7L).build());
        TestTenantEntity entity = new TestTenantEntity();

        handler(true, null).insertFill(meta(entity));

        assertThat(entity.getTenantId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("INSERT：多租户开启但无租户上下文且未配默认租户时 fail-closed 抛错")
    void insertFillTenantFailClosed() {
        TestTenantEntity entity = new TestTenantEntity();

        assertThatThrownBy(() -> handler(true, null).insertFill(meta(entity)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未获取到租户上下文");
    }

    @Test
    @DisplayName("INSERT：多租户开启、无上下文但配置了默认租户时写入默认租户")
    void insertFillUsesDefaultTenant() {
        TestTenantEntity entity = new TestTenantEntity();

        handler(true, 99L).insertFill(meta(entity));

        assertThat(entity.getTenantId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("INSERT：多租户关闭时即使有上下文也不填充 tenantId")
    void insertFillSkipsTenantWhenDisabled() {
        SecurityContextHolder.setContext(SecurityContext.builder().username("acct").tenantId(7L).build());
        TestTenantEntity entity = new TestTenantEntity();

        handler(false, null).insertFill(meta(entity));

        assertThat(entity.getTenantId()).isNull();
    }
}
