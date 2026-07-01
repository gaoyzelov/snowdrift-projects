package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;


@Slf4j
public class DataPermissionHandler implements MultiDataPermissionHandler {


    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        return null;
    }
}
