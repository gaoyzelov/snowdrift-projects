package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.result.ResultCode;
import com.snowdrift.framework.common.util.DateTimeUtil;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.annotation.ApiLog;
import com.snowdrift.framework.log.dto.ApiLogCreateDTO;
import com.snowdrift.framework.log.service.ILogService;
import com.snowdrift.framework.log.util.LogTraceUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ApiLogAspect
 *
 * @author 83674
 * @date 2026/5/7-15:37
 * @description 接口访问日志注解
 * @since 1.0.0
 */
@Slf4j
@Aspect
public class ApiLogAspect {

    @Resource
    private ILogService logService;

    @Value("${spring.application.name:unknown}")
    private String appName;

    /**
     * 环绕通知
     *
     * @param joinPoint  切点
     * @param apiLogAnno 接口日志注解
     * @return 返回结果
     * @throws Throwable 异常
     */
    @Around("@annotation(apiLogAnno)")
    public Object around(ProceedingJoinPoint joinPoint, ApiLog apiLogAnno) throws Throwable {
        StopWatch stopWatch = StopWatch.createStarted();
        Object result = null;
        Exception exception = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            stopWatch.stop();
            try {
                handleApiLog(joinPoint, apiLogAnno, result, exception, stopWatch);
            } catch (Exception e) {
                log.error("记录接口日志异常", e);
            }
        }
        return result;
    }

    /**
     * 处理接口日志
     *
     * @param joinPoint  切点
     * @param apiLogAnno 接口日志注解
     * @param result     返回结果
     * @param exception  异常
     * @param stopWatch  计时器
     */
    private void handleApiLog(JoinPoint joinPoint, ApiLog apiLogAnno, Object result, Exception exception, StopWatch stopWatch) {
        if (apiLogAnno == null || !apiLogAnno.enable()) {
            return;
        }
        HttpContext httpContext = HttpContextHolder.getContext();
        ApiLogCreateDTO apiLogDTO = ApiLogCreateDTO.builder()
                .traceId(LogTraceUtil.getTraceId())
                .appName(appName)
                .bizModule(apiLogAnno.module())
                .bizType(apiLogAnno.bizType().getCode())
                .summary(apiLogAnno.summary())
                .method(httpContext.getMethod())
                .uri(httpContext.getUri())
                .ip(httpContext.getIp())
                .requestParams(getRequestParams(apiLogAnno, joinPoint.getArgs(), httpContext.getParamMap()))
                .responseBody(getResponseBody(apiLogAnno, result))
                .ua(httpContext.getUserAgent())
                .duration(stopWatch.getDuration().toMillis())
                .operateTime(DateTimeUtil.timestampToLocalDateTime(stopWatch.getStartInstant().toEpochMilli()))
                .build();

        // 保存用户信息
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null) {
            apiLogDTO.setUserId(securityContext.getUserId());
            apiLogDTO.setTenantId(securityContext.getTenantId());
            apiLogDTO.setOperator(securityContext.getNickname());
        }

        // 判断是否存在异常
        if (Objects.nonNull(exception)) {
            apiLogDTO.setStatus(ResultCode.ERR.code());
            apiLogDTO.setErrorMsg(ExceptionUtils.getRootCauseMessage(exception));
        } else {
            apiLogDTO.setStatus(ResultCode.OK.code());
        }

        // 保存接口日志
        logService.saveApiLog(apiLogDTO);
    }

    /**
     * 获取请求参数
     *
     * @param apiLogAnno 接口日志注解
     * @param args       方法参数
     * @param paramMap   URL参数
     * @return 请求参数 JSON 字符串
     */
    private String getRequestParams(ApiLog apiLogAnno, Object[] args, Map<String, String> paramMap) {
        if (!apiLogAnno.saveParams() || (ArrayUtils.isEmpty(args) && MapUtils.isEmpty(paramMap))) {
            return StrConst.EMPTY;
        }
        boolean hasArgs = ArrayUtils.isNotEmpty(args);
        boolean hasParams = MapUtils.isNotEmpty(paramMap);
        String json;
        if (hasArgs && hasParams) {
            Map<String, Object> combined = new LinkedHashMap<>();
            Object[] filtered = filterArgs(args);
            if (ArrayUtils.isNotEmpty(filtered)) {
                combined.put("args", filtered);
            }
            combined.put("params", paramMap);
            json = safeSerialize(combined);
        } else if (hasParams) {
            json = safeSerialize(paramMap);
        } else {
            Object[] filtered = filterArgs(args);
            if (ArrayUtils.isEmpty(filtered)) {
                return StrConst.EMPTY;
            }
            json = safeSerialize(filtered);
        }
        if (ArrayUtils.isNotEmpty(apiLogAnno.mask())) {
            return applyMask(json, apiLogAnno.mask());
        }
        return json;
    }

    /**
     * 过滤不可序列化的参数类型（Servlet对象、流、文件上传等）
     */
    private Object[] filterArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> !(arg instanceof ServletRequest))
                .filter(arg -> !(arg instanceof ServletResponse))
                .filter(arg -> !(arg instanceof InputStream))
                .filter(arg -> !(arg instanceof OutputStream))
                .filter(arg -> !(arg instanceof Reader))
                .filter(arg -> !(arg instanceof MultipartFile))
                .filter(arg -> !(arg instanceof Part))
                .toArray();
    }

    /**
     * 对 JSON 字符串做深度脱敏，正则匹配任意嵌套层级的指定字段
     *
     * @param json       JSON 字符串
     * @param maskFields 需要脱敏的字段名
     * @return 脱敏后的 JSON 字符串
     */
    private String applyMask(String json, String[] maskFields) {
        if (StringUtils.isBlank(json)){
            return json;
        }
        for (String field : maskFields) {
            json = json.replaceAll(
                    "(?i)\"" + Pattern.quote(field) + "\"\\s*:\\s*(\"[^\"]*\"|[^\\s,\\[\\]{}]+)",
                    "\"" + field + "\":\"******\"");
        }
        return json;
    }

    /**
     * 获取返回结果
     *
     * @param apiLogAnno 接口日志注解
     * @param result     返回结果
     * @return 返回结果
     */
    private String getResponseBody(ApiLog apiLogAnno, Object result) {
        if (!apiLogAnno.saveResult() || Objects.isNull(result)) {
            return StrConst.EMPTY;
        }
       return safeSerialize(result);
    }

    /**
     * 安全 JSON 序列化
     */
    private String safeSerialize(Object obj) {
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("请求参数序列化失败", e);
            return StrConst.EMPTY;
        }
    }

}
