package com.snowdrift.orm.mybatisplus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.snowdrift.core.constant.StrConst;
import com.snowdrift.core.context.SecurityContextHolder;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MybatisPlusMetaObjectHandler
 *
 * @author gaoye
 * @date 2025/03/20 19:04:50
 * @description 原始对象自动填充处理器(不满足需求可自行注入)
 * @since 1.0.0
 */
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_BY = "createBy";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_BY = "updateBy";
    private static final String UPDATE_TIME = "updateTime";
    private static final String TENANT_ID = "tenantId";

    /**
     * 新增时自动填充
     *
     * @param metaObject MetaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, CREATE_BY, String.class, SecurityContextHolder.getName(StrConst.NULL));
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, TENANT_ID, String.class, SecurityContextHolder.getTenantId(String.class));
    }

    /**
     * 更新是自动填充
     *
     * @param metaObject MetaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATE_BY, String.class, SecurityContextHolder.getName(StrConst.NULL));
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }
}