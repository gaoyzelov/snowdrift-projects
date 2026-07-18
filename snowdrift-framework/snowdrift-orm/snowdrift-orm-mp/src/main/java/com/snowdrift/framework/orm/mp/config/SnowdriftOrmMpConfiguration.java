package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import com.snowdrift.framework.orm.mp.handler.CryptoKeyHolder;
import com.snowdrift.framework.orm.mp.handler.DataScopeHandler;
import com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import com.snowdrift.framework.orm.mp.properties.OrmMpBaseProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpPaginationProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({OrmMpBaseProperties.class, OrmMpTenantProperties.class, OrmMpPaginationProperties.class})
public class SnowdriftOrmMpConfiguration {

    /**
     * AES 加密密钥初始化
     *
     * @param properties ORM 基础配置属性
     */
    @PostConstruct
    public void cryptoKeyInitializer(OrmMpBaseProperties properties) {
        if (Boolean.FALSE.equals(properties.getCrypto())){
            return;
        }
        String key = properties.getCryptoKey();
        if (StringUtils.isBlank(key)) {
            throw new BizException("snowdrift.orm.mp.crypto=true 但 cryptoKey 未配置!");
        }
        int keyBytes = key.length() / 2;
        if (keyBytes != 16 && keyBytes != 24 && keyBytes != 32) {
            throw new BizException("snowdrift.orm.mp.cryptoKey 长度无效，请使用 16/24/32 位十六进制字符!");
        }
        CryptoKeyHolder.setKey(key);
        log.info("AES 加密密钥已初始化，密钥长度: {} 位", keyBytes * 8);
    }

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
            // 不指定，给定默认租户ID字段值
            if (StringUtils.isBlank(properties.getTenantIdColumn())) {
                throw new BizException("snowdrift.orm.mp.tenant.enabled=true, 但未配置租户ID字段");
            }
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
}
