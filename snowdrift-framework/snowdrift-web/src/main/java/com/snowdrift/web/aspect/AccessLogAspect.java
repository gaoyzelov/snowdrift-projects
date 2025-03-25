package com.snowdrift.web.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.core.constant.StrConst;
import com.snowdrift.core.context.SecurityContextHolder;
import com.snowdrift.core.enums.ResultEnum;
import com.snowdrift.core.utils.DesensitizeUtil;
import com.snowdrift.core.utils.NetUtil;
import com.snowdrift.core.utils.SpringUtil;
import com.snowdrift.web.anno.AccessLog;
import com.snowdrift.web.bo.WebLog;
import com.snowdrift.web.handler.IAccessLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Optional;

/**
 * AccessLogAspect
 *
 * @author gaoye
 * @date 2025/03/24 13:56:02
 * @description 访问日志切面
 * @since 1.0.0
 */
@Slf4j
@Aspect
public class AccessLogAspect {

    /**
     * 环绕通知
     *
     * @param joinPoint 切点
     * @param accessLog 访问日志注解
     * @return Object
     */
    @Around("@annotation(accessLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, AccessLog accessLog) throws Throwable {
        WebLog webLog = createAccessLog(joinPoint, accessLog);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            webLog.setResult(ResultEnum.ERR);
            webLog.setException(getExceptionStackContent(e));
            throw e;
        } finally {
            stopWatch.stop();
            webLog.setRequestCost(stopWatch.getTotalTimeMillis());
            Optional.ofNullable(SpringUtil.getBean(IAccessLogHandler.class))
                    .ifPresent(accessLogHandler -> accessLogHandler.handle(webLog));
        }
        return result;
    }

    /**
     * 创建访问日志
     *
     * @param joinPoint 切点
     * @param accessLog 注解
     */
    private WebLog createAccessLog(ProceedingJoinPoint joinPoint, AccessLog accessLog) {
        WebLog webLog = new WebLog();
        // 接收到请求，记录请求内容
        HttpServletRequest request = ((ServletRequestAttributes) Objects
                .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        // 获取被拦截方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = className + StrConst.DOT + signature.getMethod().getName();
        // 填充日志
        webLog.setType(accessLog.type());
        webLog.setModule(accessLog.module());
        webLog.setAction(accessLog.value());
        webLog.setMethod(methodName);
        webLog.setRequestUri(request.getRequestURI());
        webLog.setRequestMethod(request.getMethod());
        webLog.setRequestParams(getRequestParams(joinPoint.getArgs(), accessLog.mask()));
        webLog.setRequestIp(NetUtil.getRequestIp(request));
        webLog.setRequestBy(SecurityContextHolder.getName(StrConst.NULL));
        return webLog;
    }

    /**
     * 获取请求参数
     *
     * @param args  参数
     * @param masks 脱敏字段数组
     * @return String
     */
    private String getRequestParams(Object[] args, String[] masks) {
        if (ArrayUtils.isEmpty(args)) {
            return StrConst.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            // 判断是否为文件
            if (arg instanceof MultipartFile) {
                sb.append("[").append(((MultipartFile) arg).getOriginalFilename()).append("]");
                continue;
            }
            // 脱敏处理
            if (ArrayUtils.isEmpty(masks)) {
                sb.append(JSON.toJSONString(arg));
                continue;
            }
            try {
                JSONObject json = JSON.parseObject(JSON.toJSONString(arg));
                for (String mask : masks) {
                    json.putIfAbsent(mask, DesensitizeUtil.password(json.getString(mask)));
                }
                sb.append(json.toJSONString());
            } catch (Exception e) {
                sb.append(JSON.toJSONString(arg));
            }
        }
        return sb.toString();
    }

    /**
     * 获取异常堆栈信息
     *
     * @param e 异常
     * @return String
     */
    private String getExceptionStackContent(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName())
                .append(StrConst.COLON)
                .append(e.getMessage())
                .append("\n\t");
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            sb.append(ste).append("\n\t");
        }
        return sb.toString();
    }
}