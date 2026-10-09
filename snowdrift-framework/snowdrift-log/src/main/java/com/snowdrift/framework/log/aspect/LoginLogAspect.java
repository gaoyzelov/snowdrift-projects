package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.log.annotation.LoginLog;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.ILogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * LoginLogAspect
 *
 * @author gaoyzelov
 * @date 2026/5/7-15:33
 * @description 登录日志切面
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(100)
public class LoginLogAspect {

    private final ILogService logService;

    public LoginLogAspect(ILogService logService) {
        this.logService = logService;
    }

    /**
     * 登录后置通知
     */
    @AfterReturning(pointcut = "@annotation(loginLogAnno)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, LoginLog loginLogAnno, Object result) {
        try {
            handleLoginLog(joinPoint, loginLogAnno, null, result);
        } catch (Throwable e) {
            // 只 catch Exception 会让它穿透通知，把成功的登录响应变成异常
            log.error("记录登录日志异常", e);
        }
    }

    /**
     * 登录异常通知
     */
    @AfterThrowing(pointcut = "@annotation(loginLogAnno)", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, LoginLog loginLogAnno, Throwable exception) {
        try {
            handleLoginLog(joinPoint, loginLogAnno, exception, null);
        } catch (Throwable e) {
            log.error("记录登录日志异常", e);
        }
    }

    /**
     * 处理登录日志
     *
     * @param joinPoint    切点
     * @param loginLogAnno 登录注解
     * @param exception    异常信息
     * @param result       登录方法返回值
     */
    private void handleLoginLog(JoinPoint joinPoint, LoginLog loginLogAnno, Throwable exception, Object result) {
        if (loginLogAnno == null || !loginLogAnno.enable()) {
            return;
        }
        // 请求信息（非 HTTP 场景为 null）
        HttpContext context = HttpContextHolder.getContext();
        boolean hasHttpContext = context != null;
        // 登录日志初始化
        LoginLogHolder holder = LoginLogHolder.builder()
                .username(getUsername(joinPoint,loginLogAnno.accountField()))
                .ip(hasHttpContext ? context.getIp() : null)
                .ipLocation(hasHttpContext ? context.getIpLocation() : null)
                .ua(hasHttpContext ? context.getUserAgent() : null)
                .loginTime(LocalDateTime.now())
                .build();

        // 判断是否存在登录异常
        if (Objects.nonNull(exception)) {
            holder.setStatus(ResultCode.ERR.code());
            holder.setMsg(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
        } else if (result instanceof Result<?> r && !Objects.equals(ResultCode.OK.code(), r.getCode())) {
            holder.setStatus(ResultCode.ERR.code());
            holder.setMsg(r.getMsg() != null ? r.getMsg() : ResultCode.ERR.msg());
        } else {
            holder.setStatus(ResultCode.OK.code());
            holder.setMsg("登录成功");
        }
        // 保存登录日志
        logService.saveLoginLog(holder);
    }

    /**
     * 获取登录用户名
     *
     * @param joinPoint    切点
     * @param accountField 登录用户名字段
     * @return 用户名，提取不到时为 {@link StrConst#UNKNOWN}
     */
    private String getUsername(JoinPoint joinPoint, String accountField) {
        Object[] args = joinPoint.getArgs();
        if (ArrayUtils.isEmpty(args) || StringUtils.isBlank(accountField)) {
            return StrConst.UNKNOWN;
        }
        // 1. 对象入参按字段名提取
        for (Object arg : args) {
            String value = readField(arg, accountField);
            if (value != null) {
                return value;
            }
        }
        // 2. 裸参数按形参名提取
        String[] parameterNames = parameterNames(joinPoint);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                if (accountField.equals(parameterNames[i]) && Objects.nonNull(args[i])) {
                    return String.valueOf(args[i]);
                }
            }
        }
        return StrConst.UNKNOWN;
    }

    /**
     * 从对象入参中按字段名读取账号；入参不是可解析对象或字段不存在时返回 null
     *
     * @param arg          单个方法入参
     * @param accountField 字段名
     * @return 账号，取不到时为 null
     */
    private String readField(Object arg, String accountField) {
        if (Objects.isNull(arg) || isScalar(arg)) {
            return null;
        }
        try {
            JSONObject jsonObj = JSONObject.from(arg);
            return jsonObj.containsKey(accountField) ? jsonObj.getString(accountField) : null;
        } catch (Throwable e) {
            // 无法转换为 JSON 属于常规情况（如 String[]、Servlet 对象）；自引用对象还会让 fastjson2
            // 抛 StackOverflowError。统一降为 debug 并跳过，避免刷 ERROR 堆栈或影响登录流程
            log.debug("登录入参无法转换为 JSON，跳过按字段名提取账号：type={}", arg.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 判断是否为标量入参（标量没有“字段”可提取）
     */
    private static boolean isScalar(Object arg) {
        return arg instanceof CharSequence || arg instanceof Number || arg instanceof Boolean
                || arg instanceof Character || arg instanceof Enum<?>;
    }

    /**
     * 获取方法形参名（依赖编译期 -parameters），不可用时返回 null
     *
     * @param joinPoint 切点
     * @return 形参名数组或 null
     */
    private String[] parameterNames(JoinPoint joinPoint) {
        try {
            if (joinPoint.getSignature() instanceof MethodSignature signature) {
                return signature.getParameterNames();
            }
        } catch (RuntimeException e) {
            log.debug("当前环境无法获取方法形参名，登录账号将退回 unknown", e);
        }
        return null;
    }

}