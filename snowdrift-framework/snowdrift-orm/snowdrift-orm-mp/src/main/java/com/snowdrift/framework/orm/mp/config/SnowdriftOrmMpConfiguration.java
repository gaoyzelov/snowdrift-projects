package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.snowdrift.framework.orm.mp.handler.DataPermissionHandler;
import com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import com.snowdrift.framework.orm.mp.plugins.DataCryptoInterceptor;
import com.snowdrift.framework.orm.mp.properties.OrmMpCryptoProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpPaginationProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift ORM MyBatis-Plus 自动配置
 * <p>
 * 启用以下能力（均支持通过属性开关控制）：
 * <ul>
 *   <li><b>分页</b>：基于 {@link PaginationInnerInterceptor}，支持多数据库方言、最大条数限制、溢出处理</li>
 *   <li><b>多租户</b>：基于 {@link TenantLineInnerInterceptor} + {@link com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler}，自动在 SQL 中注入租户过滤条件</li>
 *   <li><b>乐观锁</b>：基于 {@link OptimisticLockerInnerInterceptor}，配合实体字段上的 {@code @Version} 注解</li>
 *   <li><b>自动填充</b>：基于 {@link MetaObjectHandler}，INSERT/UPDATE 时自动填充创建人、创建时间、更新人、更新时间、租户ID</li>
 *   <li><b>防全表操作</b>：基于 {@link BlockAttackInnerInterceptor}，阻止不带 WHERE 条件的 UPDATE/DELETE</li>
 *   <li><b>字段加解密</b>：基于 {@link com.snowdrift.framework.orm.mp.plugins.DataCryptoInterceptor}，对 {@code @Encrypted} 字段自动 AES 加解密</li>
 *   <li><b>数据权限</b>：基于 {@link DataPermissionInterceptor} + {@link DataPermissionHandler}</li>
 * </ul>
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/1-14:55
 * @description MyBatis Plus 配置类
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({OrmMpCryptoProperties.class, OrmMpTenantProperties.class, OrmMpPaginationProperties.class})
public class SnowdriftOrmMpConfiguration {

    /**
     * MyBatis-Plus 核心拦截器
     * <p>
     * 统一管理所有内部插件（InnerInterceptor），按添加顺序执行：
     * <ol>
     *   <li>多租户插件 — 自动追加租户过滤条件（若启用）</li>
     *   <li>数据权限插件 — 根据用户数据范围自动追加行级过滤条件（若启用）</li>
     *   <li>乐观锁插件 — 更新时检查版本号</li>
     *   <li>分页插件 — 物理分页与 count 优化</li>
     * </ol>
     * </p>
     *
     * @param paginationProperties 分页配置属性
     * @param tenantProperties     多租户配置属性
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(OrmMpPaginationProperties paginationProperties,
                                                         OrmMpTenantProperties tenantProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件
        TenantLineInnerInterceptor tenantInterceptor = this.getTenantLineInnerInterceptor(tenantProperties);
        if (tenantInterceptor != null) {
            interceptor.addInnerInterceptor(tenantInterceptor);
        }
        // 数据权限插件
            interceptor.addInnerInterceptor(new DataPermissionInterceptor());
        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

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
     * <p>
     * 当 {@code snowdrift.orm.mp.tenant.enabled=true} 时返回配置好的插件；
     * 否则返回 {@code null}，{@link #mybatisPlusInterceptor} 中跳过该插件注册。
     * </p>
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
     * <p>
     * INSERT 时填充 createBy、createTime、updateBy、updateTime、tenantId（若多租户启用）<br>
     * UPDATE 时填充 updateBy、updateTime
     * </p>
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
     * <p>
     * 对标注 {@link com.snowdrift.framework.orm.core.anno.Encrypted @Encrypted} 的字段：
     * <ul>
     *   <li>写入前自动 AES 加密</li>
     *   <li>读取后自动 AES 解密</li>
     * </ul>
     * 通过 {@code snowdrift.orm.mp.crypto.enabled=true} 开启。
     * </p>
     *
     * @param properties 加密配置属性（含 AES 密钥）
     * @return DataCryptoInterceptor 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "snowdrift.orm.mp.crypto", name = "enabled", havingValue = "true")
    public DataCryptoInterceptor dataCryptoInterceptor(OrmMpCryptoProperties properties) {
        return new DataCryptoInterceptor(properties);
    }
}
