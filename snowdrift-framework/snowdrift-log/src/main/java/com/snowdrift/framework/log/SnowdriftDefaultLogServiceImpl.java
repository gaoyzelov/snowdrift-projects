package com.snowdrift.framework.log;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * SnowdriftDefaultLogServiceImpl
 *
 * @author gaoyzelov
 * @date 2026/9/4-09:24
 * @description 默认日志服务实现：不落库，仅在 debug 级别把三类日志以 JSON 输出到应用日志
 * @since 1.0.0
 */
@Slf4j
public class SnowdriftDefaultLogServiceImpl implements ILogService {

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
