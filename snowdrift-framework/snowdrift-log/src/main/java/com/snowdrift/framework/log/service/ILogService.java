package com.snowdrift.framework.log.service;

import com.snowdrift.framework.log.dto.ApiLogCreateDTO;
import com.snowdrift.framework.log.dto.LoginLogCreateDTO;
import com.snowdrift.framework.log.dto.OperateLogCreateDTO;

/**
 *  ILogService
 * @author gaoyzelov
 * @date 2026/4/30-14:54
 * @description 日志服务接口
 * @since 1.0.0
 */
public interface ILogService {

    /**
     * 保存接口日志
      * @param apiLogCreateDTO 创建接口日志DTO
     */
    void saveApiLog(ApiLogCreateDTO apiLogCreateDTO);

    /**
     * 保存登录日志
      * @param loginLogCreateDTO 创建登录日志DTO
     */
    void saveLoginLog(LoginLogCreateDTO loginLogCreateDTO);

    /**
     * 保存操作日志
     * @param operateLogCreateDTO 创建操作日志DTO
     */
    void saveOperateLog(OperateLogCreateDTO operateLogCreateDTO);
}
