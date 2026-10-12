package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.orm.core.scope.IDataScopeProvider;
import com.snowdrift.framework.orm.mp.CryptoKeyHolder;
import com.snowdrift.framework.orm.mp.handler.DataScopeHandler;
import com.snowdrift.framework.orm.mp.handler.FieldAutoFillHandler;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import com.snowdrift.framework.orm.mp.properties.OrmMpBaseProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpPaginationProperties;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.HexFormat;
import java.util.Optional;

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

    private final OrmMpBaseProperties baseProperties;

    public SnowdriftOrmMpConfiguration(OrmMpBaseProperties baseProperties) {
        this.baseProperties = baseProperties;
    }

    /**
     * AES 加密密钥初始化
     * <p>JSR-250 规范要求 @PostConstruct 方法必须无参，属性通过构造器注入。</p>
     */
    @PostConstruct
    public void cryptoKeyInitializer() {
        if (Boolean.FALSE.equals(baseProperties.getCrypto())) {
            return;
        }
        String key = baseProperties.getCryptoKey();
        if (StringUtils.isBlank(key)) {
            throw new BizException("数据加密密钥未配置");
        }
        // 十六进制密钥必须为偶数位，且对应 AES-128/192/256
        if (key.length() % 2 != 0) {
            throw new BizException("数据加密密钥长度无效（十六进制字符串需为偶数位）");
        }
        int keyBytes = key.length() / 2;
        if (keyBytes != 16 && keyBytes != 24 && keyBytes != 32) {
            throw new BizException("数据加密密钥长度无效");
        }
        // 长度合法但非十六进制字符的密钥（如 32 个 'z'）会通过上面的长度校验，
        // 却在首次加密写库时才由 EncryptUtil.parseHexKey 抛错（fail-late）。
        // 此处与运行期一致地用 HexFormat 校验，把配置错误提前到启动期暴露。
        try {
            HexFormat.of().parseHex(key);
        } catch (IllegalArgumentException e) {
            throw new BizException("数据加密密钥必须是合法的十六进制字符串");
        }
        CryptoKeyHolder.setKey(key);
        log.info("AES 加密密钥已初始化，密钥长度: {} 位。提示：本开关只初始化密钥、不自动加密任何字段，"
                + "需要加解密的字段请在实体上标注 @TableField(typeHandler = AesEncryptTypeHandler.class)，"
                + "并确保实体类标注 @TableName(autoResultMap = true)", keyBytes * 8);
    }

    /**
     * MyBatis-Plus 核心拦截器
     *
     * @param paginationProperties  分页配置属性
     * @param tenantProperties      多租户配置属性
     * @param optProvider     数据权限提供者（可选，业务应用实现后可自动注入）
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(OrmMpPaginationProperties paginationProperties, OrmMpTenantProperties tenantProperties, Optional<IDataScopeProvider> optProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 防止全表更新与删除插件。
        // 排在租户拦截器之前：租户拦截器会先把 `UPDATE t SET x = 1` 改写成带 WHERE 的语句，
        // 排在它后面时 BlockAttackInnerInterceptor 读到的是已改写的 SQL，守卫不再触发，
        // 其承诺的“阻止不带 WHERE 的 UPDATE/DELETE”形同虚设。
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 多租户插件（使用 MyBatis-Plus 默认的 TenantLineInnerInterceptor）
        if (Boolean.TRUE.equals(tenantProperties.getEnabled())) {
            interceptor.addInnerInterceptor(this.getTenantLineInnerInterceptor(new MultiTenantLineHandler(tenantProperties)));
        }
        // 数据权限插件（始终注册；未配置 IDataScopeProvider 时，由 DataScopeHandler 对携带 @DataScope 的语句显式抛错）
        interceptor.addInnerInterceptor(this.getDataPermissionInterceptor(optProvider));

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
     * 构建数据权限插件（始终注册）
     * <p>未装配 {@link IDataScopeProvider} 时仍注册拦截器，由 {@link DataScopeHandler}
     * 在语句实际携带 {@code @DataScope} 时显式抛错，避免安全控制静默失效。</p>
     *
     * @param optProvider 数据权限提供者（可为空）
     * @return 数据权限插件实例
     */
    private DataPermissionInterceptor getDataPermissionInterceptor(Optional<IDataScopeProvider> optProvider) {
        DataScopeHandler dataScopeHandler = new DataScopeHandler(optProvider.orElse(null));
        return new DataPermissionInterceptor(dataScopeHandler);
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
     * 构建多租户插件
     *
     * @param tenantHandler 多租户行级处理器（与语句守卫共用同一实例，保证忽略表清单一致）
     * @return 多租户插件实例
     */
    private TenantLineInnerInterceptor getTenantLineInnerInterceptor(MultiTenantLineHandler tenantHandler) {
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(tenantHandler);
        return tenantInterceptor;
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
