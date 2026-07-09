package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import com.snowdrift.framework.orm.mp.handler.DataScopeHandler;
import com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import com.snowdrift.framework.orm.mp.plugins.DataCryptoInterceptor;
import com.snowdrift.framework.orm.mp.properties.OrmMpBaseProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpPaginationProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift ORM MyBatis-Plus 自动配置
 *
 * @author gaoyzelov
 * @date 2026/7/1-14:55
 * @description MyBatis Plus 配置类
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({OrmMpBaseProperties.class, OrmMpTenantProperties.class, OrmMpPaginationProperties.class})
public class SnowdriftOrmMpConfiguration {

    /**
     * MyBatis-Plus 核心拦截器
     *
     * @param paginationProperties  分页配置属性
     * @param tenantProperties      多租户配置属性
     * @param dataScopeProvider     数据权限提供者（可选，业务应用实现后可自动注入）
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(OrmMpBaseProperties baseProperties, OrmMpPaginationProperties paginationProperties,
                                                         OrmMpTenantProperties tenantProperties,
                                                         ObjectProvider<IDataScopeProvider> dataScopeProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件
        TenantLineInnerInterceptor tenantInterceptor = this.getTenantLineInnerInterceptor(tenantProperties);
        if (tenantInterceptor != null) {
            interceptor.addInnerInterceptor(tenantInterceptor);
        }
        // 数据权限插件（无 provider 时自动降级：DEPT_AND_SUB→DEPT，CUSTOM→SELF）
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataScopeHandler(dataScopeProvider.getIfAvailable())));

        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 乐观锁插件
        if (Boolean.TRUE.equals(baseProperties.getOptimisticLock())){
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = this.getPaginationInnerInterceptor(paginationProperties);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * 构建分页插件
     *
     * @param properties 分页配置属性
     * @return 分页插件实例
     */
    private PaginationInnerInterceptor getPaginationInnerInterceptor(OrmMpPaginationProperties properties) {
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(properties.getDbType());
        paginationInterceptor.setMaxLimit(properties.getMaxLimit());
        paginationInterceptor.setOverflow(properties.getOverflow());
        paginationInterceptor.setOptimizeJoin(properties.getOptimizeJoin());
        return paginationInterceptor;
    }

    /**
     * 构建多租户插件（仅在启用时返回实例）
     *
     * @param properties 多租户配置属性
     * @return 多租户插件实例，未启用时返回 {@code null}
     */
    private TenantLineInnerInterceptor getTenantLineInnerInterceptor(OrmMpTenantProperties properties) {
        if (Boolean.TRUE.equals(properties.getEnabled())) {
            TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
            MultiTenantLineHandler tenantHandler = new MultiTenantLineHandler(properties);
            tenantInterceptor.setTenantLineHandler(tenantHandler);
            return tenantInterceptor;
        }
        return null;
    }


    /**
     * 字段自动填充处理器
     *
     * @param properties 多租户配置属性（用于判断是否填充租户ID）
     * @return MetaObjectHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler fieldAutoFillHandler(OrmMpTenantProperties properties) {
        return new FieldAutoFillHandler(properties);
    }

    /**
     * 数据加解密拦截器（仅在配置启用时注册）
     *
     * @param properties 加密配置属性（含 AES 密钥）
     * @return DataCryptoInterceptor 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "snowdrift.orm", name = "crypto", havingValue = "true")
    public DataCryptoInterceptor dataCryptoInterceptor(OrmMpBaseProperties properties) {
        return new DataCryptoInterceptor(properties);
    }
}
