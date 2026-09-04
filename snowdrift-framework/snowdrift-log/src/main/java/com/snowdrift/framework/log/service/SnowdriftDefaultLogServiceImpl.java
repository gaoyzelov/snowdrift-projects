package com.snowdrift.framework.log.service;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author 83674
 * @date 2026/9/4-09:24
 * @description
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftDefaultLogServiceImpl implements ILogService{

    @Override
    public void saveApiLog(ApiLogHolder apiLog) {
        if (log.isDebugEnabled()){
            log.debug("接口日志：{}", JSON.toJSONString(apiLog));
        }
    }

    @Override
    public void saveLoginLog(LoginLogHolder holder) {
        if (log.isDebugEnabled()){
            log.debug("登录日志：{}", JSON.toJSONString(holder));
        }
    }

    @Override
    public void saveOperateLog(OperateLogHolder holder) {
        if (log.isDebugEnabled()){
            log.debug("操作日志：{}", JSON.toJSONString(holder));
        }
    }
}
