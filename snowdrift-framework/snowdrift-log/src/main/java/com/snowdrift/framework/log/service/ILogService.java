package com.snowdrift.framework.log.service;

import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;

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
      * @param holder 创建接口日志DTO
     */
    void saveApiLog(ApiLogHolder holder);

    /**
     * 保存登录日志
      * @param holder 创建登录日志DTO
     */
    void saveLoginLog(LoginLogHolder holder);

    /**
     * 保存操作日志
     * @param holder 创建操作日志DTO
     */
    void saveOperateLog(OperateLogHolder holder);
}
