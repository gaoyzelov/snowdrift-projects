package com.snowdrift.orm.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.snowdrift.orm.mybatisplus.handler.MyBatisPlusTenantLineHandler;
import com.snowdrift.orm.mybatisplus.handler.MybatisPlusMetaObjectHandler;
import com.snowdrift.orm.mybatisplus.interceptor.DesensitizeInterceptor;
import com.snowdrift.orm.mybatisplus.properties.MybatisPlusTenantProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlusOrmAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/20 19:03:19
 * @description mybatis-plus自动配置
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusTenantProperties.class)
public class MybatisPlusOrmAutoConfiguration {

    /**
     * mybatis plus 插件配置
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusTenantProperties tenantProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件
        if (Boolean.TRUE.equals(tenantProperties.getEnabled())) {
            MyBatisPlusTenantLineHandler tenantLineHandler = new MyBatisPlusTenantLineHandler(tenantProperties);
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        }
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        return interceptor;
    }


    /**
     * 分页插件
     *
     * @return PaginationInnerInterceptor
     */
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        // 设置数据库类型
        paginationInnerInterceptor.setDbType(DbType.MYSQL);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInnerInterceptor.setMaxLimit(500L);
        // 溢出总页数后是否进行处理
        paginationInnerInterceptor.setOverflow(true);
        return paginationInnerInterceptor;
    }

    /**
     * mybatis plus 自动填充
     *
     * @return MyBatisPlusMetaObjectHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler myBatisPlusMetaObjectHandler() {
        return new MybatisPlusMetaObjectHandler();
    }

    /**
     * 数据脱敏插件
     *
     * @return SensitiveInterceptor
     */
    @Bean
    public DesensitizeInterceptor desensitizeInterceptor() {
        return new DesensitizeInterceptor();
    }

}