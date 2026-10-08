package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.base.util.DateTimeUtil;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.annotation.ApiLog;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.util.LogTraceUtil;
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
import org.springframework.core.annotation.Order;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ApiLogAspect
 *
 * @author gaoyzelov
 * @date 2026/5/7-15:37
 * @description 接口访问日志注解
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(100)
public class ApiLogAspect {

    /**
     * JSON 脱敏的最大递归深度，防止恶意深度嵌套 JSON 导致 StackOverflow
     */
    private static final int MAX_MASK_DEPTH = 10;

    private final ILogService logService;

    @Value("${spring.application.name:unknown}")
    private String appName;

    public ApiLogAspect(ILogService logService) {
        this.logService = logService;
    }

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
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            // 同时捕获 Error，避免异常状态漏记（仍会原样抛出）
            exception = t;
            throw t;
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
    private void handleApiLog(JoinPoint joinPoint, ApiLog apiLogAnno, Object result, Throwable exception, StopWatch stopWatch) {
        if (apiLogAnno == null || !apiLogAnno.enable()) {
            return;
        }
        // 请求信息（非 HTTP 场景为 null）
        HttpContext httpContext = HttpContextHolder.getContext();
        boolean hasHttpContext = httpContext != null;
        // 用户信息：匿名等无安全上下文场景允许为 null，不影响接口日志记录
        SecurityContext securityContext = SecurityContextHolder.peekContext();
        String operator = securityContext != null && StringUtils.isNotBlank(securityContext.getNickname())
                ? securityContext.getNickname()
                : (securityContext != null ? securityContext.getUsername() : null);
        // API 日志初始化
        ApiLogHolder holder = ApiLogHolder.builder()
                .traceId(LogTraceUtil.getTraceId())
                .appName(appName)
                .bizModule(apiLogAnno.module())
                .bizType(apiLogAnno.bizType().getCode())
                .summary(apiLogAnno.summary())
                .method(hasHttpContext ? httpContext.getMethod() : null)
                .uri(hasHttpContext ? httpContext.getUri() : null)
                .requestParams(getRequestParams(apiLogAnno, joinPoint.getArgs(),
                        hasHttpContext ? httpContext.getParamMap() : Map.of()))
                .ip(hasHttpContext ? httpContext.getIp() : null)
                .ua(hasHttpContext ? httpContext.getUserAgent() : null)
                .responseBody(getResponseBody(apiLogAnno, result))
                .duration(stopWatch.getDuration().toMillis())
                .userId(securityContext != null ? securityContext.getUserId() : null)
                .tenantId(securityContext != null ? securityContext.getTenantId() : null)
                .operator(operator)
                .operateTime(DateTimeUtil.timestampToLocalDateTime(stopWatch.getStartInstant().toEpochMilli()))
                .build();

        // 判断结果：抛异常或业务返回失败（Result.code != OK）均记为失败
        if (Objects.nonNull(exception)) {
            holder.setStatus(ResultCode.ERR.code());
            holder.setErrorMsg(ExceptionUtils.getRootCauseMessage(exception));
        } else if (result instanceof Result<?> r && !Objects.equals(ResultCode.OK.code(), r.getCode())) {
            holder.setStatus(ResultCode.ERR.code());
            holder.setErrorMsg(r.getMsg());
        } else {
            holder.setStatus(ResultCode.OK.code());
        }

        // 保存接口日志
        logService.saveApiLog(holder);
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
        return maskIfConfigured(apiLogAnno, json);
    }

    /**
     * 已配置 {@link ApiLog#mask()} 时对 JSON 脱敏，否则原样返回。
     * <p>请求参数与响应体共用本方法：同一个敏感字段不应只因出现在返回值中就漏脱敏。</p>
     *
     * @param apiLogAnno 接口日志注解
     * @param json       已序列化的 JSON 文本
     * @return 脱敏后的 JSON；未配置 mask 或文本为空时原样返回
     */
    private String maskIfConfigured(ApiLog apiLogAnno, String json) {
        if (StringUtils.isBlank(json) || ArrayUtils.isEmpty(apiLogAnno.mask())) {
            return json;
        }
        // 字段名统一按 ROOT Locale 小写：默认 Locale 下（如土耳其语）"ID" 会变成 "ıd"，导致掩码静默失效
        Set<String> maskFields = Arrays.stream(apiLogAnno.mask())
                .map(field -> field.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return applyMask(json, maskFields);
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
     * 对 JSON 做递归脱敏，将指定字段的值替换为 ******
     *
     * @param json       JSON 字符串
     * @param maskFields 需脱敏的字段名（小写）
     * @return 脱敏后的 JSON 字符串
     */
    private String applyMask(String json, Set<String> maskFields) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSON.parse(json);
            maskValue(parsed, maskFields);
            return JSON.toJSONString(parsed);
        } catch (Exception e) {
            // 不能回退原文：那等于把未脱敏的敏感数据落库，与配置 mask 的意图相反。宁可丢弃这段内容。
            log.error("JSON 脱敏失败，已放弃记录该段内容（避免未脱敏数据落库）", e);
            return StrConst.MASK_REPLACEMENT;
        }
    }

    /**
     * 递归脱敏 JSON 中的指定字段
     */
    private void maskValue(Object value, Set<String> maskFields) {
        maskValue(value, maskFields, 0);
    }

    /**
     * 递归脱敏 JSON 中的指定字段（带深度限制）
     *
     * @param value      当前处理的 JSON 节点
     * @param maskFields 需脱敏的字段名
     * @param depth      当前递归深度
     */
    private void maskValue(Object value, Set<String> maskFields, int depth) {
        // 深度保护：防止恶意深度嵌套 JSON 导致 StackOverflow。
        // 超过上限的层级在父节点处被整体置为掩码（见下），而不是原样保留——
        // 保留会把未脱敏数据写进日志，与配置 mask 的意图相反。
        if (depth > MAX_MASK_DEPTH) {
            log.warn("JSON 脱敏达到最大深度限制({})，超出部分已整体替换为掩码", MAX_MASK_DEPTH);
            return;
        }
        if (value instanceof JSONObject obj) {
            List<String> toMask = new ArrayList<>();
            for (String key : obj.keySet()) {
                if (maskFields.contains(key.toLowerCase(Locale.ROOT))) {
                    toMask.add(key);
                } else if (depth + 1 > MAX_MASK_DEPTH) {
                    obj.put(key, StrConst.MASK_REPLACEMENT);
                } else {
                    maskValue(obj.get(key), maskFields, depth + 1);
                }
            }
            for (String key : toMask) {
                obj.put(key, StrConst.MASK_REPLACEMENT);
            }
        } else if (value instanceof JSONArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                if (depth + 1 > MAX_MASK_DEPTH) {
                    arr.set(i, StrConst.MASK_REPLACEMENT);
                } else {
                    maskValue(arr.get(i), maskFields, depth + 1);
                }
            }
        }
    }

    /**
     * 获取返回结果
     * <p>序列化后与请求参数共用同一套 {@link ApiLog#mask()} 规则。</p>
     *
     * @param apiLogAnno 接口日志注解
     * @param result     返回结果
     * @return 返回结果
     */
    private String getResponseBody(ApiLog apiLogAnno, Object result) {
        if (!apiLogAnno.saveResult() || Objects.isNull(result)) {
            return StrConst.EMPTY;
        }
        return maskIfConfigured(apiLogAnno, safeSerialize(result));
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
