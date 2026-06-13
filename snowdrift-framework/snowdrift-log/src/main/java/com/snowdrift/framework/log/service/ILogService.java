package com.snowdrift.framework.log.service;

import com.snowdrift.framework.log.dto.ApiLogCreateDTO;
import com.snowdrift.framework.log.dto.LoginLogCreateDTO;
import com.snowdrift.framework.log.dto.OperateLogCreateDTO;

/**
 *  ILogService
 * @author 83674
 * @date 2026/4/30-14:54
 * @description 日志服务接口
 * @since 1.0.0
 */
public interface ILogService {

    void saveApiLog(ApiLogCreateDTO apiLogCreateDTO);

    void saveLoginLog(LoginLogCreateDTO loginLogCreateDTO);

    void saveOperateLog(OperateLogCreateDTO operateLogCreateDTO);
}
