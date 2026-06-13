package com.snowdrift.framework.log.service;

import com.snowdrift.framework.log.dto.ApiLogCreateDTO;
import com.snowdrift.framework.log.dto.LoginLogCreateDTO;
import com.snowdrift.framework.log.dto.OperateLogCreateDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * StdoutLogServiceImpl
 *
 * @author 83674
 * @date 2026/4/30-14:56
 * @description 控制台日志服务实现类
 * @since 1.0.0
 */
@Slf4j
public class StdoutLogServiceImpl implements ILogService {

    @Override
    public void saveApiLog(ApiLogCreateDTO apiLogCreateDTO) {
        log.info("[接口日志]: {}", apiLogCreateDTO);
    }

    @Override
    public void saveLoginLog(LoginLogCreateDTO loginLogCreateDTO) {
        log.info("[登录日志]: {}", loginLogCreateDTO);
    }

    @Override
    public void saveOperateLog(OperateLogCreateDTO operateLogCreateDTO) {
        log.info("[操作日志]: {}", operateLogCreateDTO);
    }
}
