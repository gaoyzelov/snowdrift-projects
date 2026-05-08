package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import java.util.*;

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
        Map<String, Object> combined = new LinkedHashMap<>();
        if (ArrayUtils.isNotEmpty(args)) {
            Arrays.stream(args)
                    .map(this::transToMap)
                    .filter(MapUtils::isNotEmpty)
                    .forEach(combined::putAll);
        }
        if (MapUtils.isNotEmpty(paramMap)) {
            combined.putAll(paramMap);
        }
        // 应用脱敏
        if (MapUtils.isNotEmpty(combined) && ArrayUtils.isNotEmpty(apiLogAnno.mask())) {
            applyMask(combined, apiLogAnno.mask());
        }
        try {
            return JSON.toJSONString(combined);
        } catch (Exception e) {
            log.error("JSON序列化请求参数失败", e);
        }
        return StrConst.EMPTY;
    }

    /**
     * 转换参数为Map
     *
     * @param arg 方法参数
     * @return 参数Map
     */
    private Map<String, Object> transToMap(Object arg) {
        if (Objects.isNull(arg) ||
                arg instanceof ServletRequest ||
                arg instanceof ServletResponse ||
                arg instanceof InputStream ||
                arg instanceof OutputStream ||
                arg instanceof MultipartFile) {
            return Collections.emptyMap();
        }
        try {
            return JSONObject.from(arg).to(Map.class);
        } catch (Exception e) {
            log.info("参数解析异常", e);
        }
        return Collections.emptyMap();
    }

    /**
     * 应用脱敏处理
     *
     * @param map        原始数据
     * @param maskFields 需要脱敏的字段
     */
    private void applyMask(Map<String, Object> map, String[] maskFields) {
        for (String field : maskFields) {
            if (map.containsKey(field)) {
                map.put(field, "******");
            }
        }
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
        try {
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("JSON序列化请求结果失败", e);
        }
        return StrConst.EMPTY;
    }
}
