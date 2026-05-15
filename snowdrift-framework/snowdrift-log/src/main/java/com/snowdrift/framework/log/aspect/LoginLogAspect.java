package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.result.ResultCode;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.log.annotation.LoginLog;
import com.snowdrift.framework.log.dto.LoginLogCreateDTO;
import com.snowdrift.framework.log.service.ILogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LoginLogAspect
 *
 * @author 83674
 * @date 2026/5/7-15:33
 * @description 登录日志切面
 * @since 1.0.0
 */
@Slf4j
@Aspect
public class LoginLogAspect {

    @Resource
    private ILogService logService;


    /**
     * 登录后置通知
     */
    @AfterReturning(pointcut = "@annotation(loginLogAnno)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, LoginLog loginLogAnno, Object result) {
        try {
            handleLoginLog(joinPoint, loginLogAnno, null);
        } catch (Exception e) {
            log.error("记录登录日志异常", e);
        }
    }

    /**
     * 登录异常通知
     */
    @AfterThrowing(pointcut = "@annotation(loginLogAnno)", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, LoginLog loginLogAnno, Exception exception) {
        try {
            handleLoginLog(joinPoint, loginLogAnno, exception);
        } catch (Exception e) {
            log.error("记录登录日志异常", e);
        }
    }

    /**
     * 处理登录日志
     *
     * @param joinPoint    切点
     * @param loginLogAnno 登录注解
     * @param exception    异常信息
     */
    private void handleLoginLog(JoinPoint joinPoint, LoginLog loginLogAnno, Exception exception) {
        if (loginLogAnno == null || !loginLogAnno.enable()) {
            return;
        }
        // 请求信息
        HttpContext context = HttpContextHolder.getContext();
        // 登录日志初始化
        LoginLogCreateDTO loginLogDTO = LoginLogCreateDTO.builder()
                .username(getUsername(joinPoint))
                .ip(context.getIp())
                .ipLocation(context.getIpLocation())
                .ua(context.getUserAgent())
                .loginTime(LocalDateTime.now())
                .build();

        // 判断是否存在登录异常
        if (Objects.nonNull(exception)) {
            loginLogDTO.setStatus(ResultCode.ERR.code());
            loginLogDTO.setMsg(exception.getMessage());
        } else {
            loginLogDTO.setStatus(ResultCode.OK.code());
            loginLogDTO.setMsg("登录成功");
        }
        // 保存登录日志
        logService.saveLoginLog(loginLogDTO);
    }

    /**
     * 获取登录用户名
     *
     * @param joinPoint 切点
     * @return 用户名
     */
    private String getUsername(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (ArrayUtils.isNotEmpty(args)) {
            for (Object arg : args) {
                if (Objects.nonNull(arg)) {
                    try {
                        return JSONObject.from(arg).getString("username", StrConst.UNKNOWN);
                    } catch (Exception e) {
                        log.error("获取登录用户信息失败", e);
                    }
                }
            }
        }
        return StrConst.UNKNOWN;
    }

}