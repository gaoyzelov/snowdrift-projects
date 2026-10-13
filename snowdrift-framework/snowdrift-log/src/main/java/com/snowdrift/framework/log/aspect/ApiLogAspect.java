package com.snowdrift.framework.log.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.framework.base.exception.BizException;
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

    /**
     * 内置的凭据字段词表（小写），按<b>后缀</b>匹配字段名，无需使用者配置
     * <p>只收较长的专用词：{@code password} 会命中 {@code userPassword}，而 {@code token} 不会命中
     * {@code tokenCount}（后者不是敏感值）。词表之外的自定义敏感字段仍由 {@link ApiLog#mask()} 声明。</p>
     */
    private static final Set<String> DEFAULT_CREDENTIAL_SUFFIXES = Set.of(
            "password", "passwd", "pwd", "secret", "token", "credential", "authorization", "privatekey");

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
            // 统一转换为 BizException，避免框架切面向外泄漏其他 Throwable 类型。
            exception = t;
            throw BizException.wrap(t);
        } finally {
            stopWatch.stop();
            try {
                handleApiLog(joinPoint, apiLogAnno, result, exception, stopWatch);
            } catch (Throwable e) {
                // 捕获 Throwable：fastjson2 对深嵌套/循环引用对象抛的是 StackOverflowError（Error），
                // 只 catch Exception 会让它穿过 finally，丢掉已成功的返回值或掩盖业务原本的异常
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
            if (StringUtils.isEmpty(json)) {
                // 个别入参仍然不可序列化（如 MultipartFile[]）时，至少保住 URL 参数，
                // 避免一个坏参数把本可记录的信息一起拖没
                json = safeSerialize(paramMap);
            }
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
     * 对 JSON 脱敏：注解 {@link ApiLog#mask()} 给出的字段名 + 内置凭据字段（见 {@link #isSensitiveKey}）。
     * <p>请求参数与响应体共用本方法：同一个敏感字段不应只因出现在返回值中就漏脱敏。</p>
     * <p>内置凭据字段无条件生效 —— 未配置 {@code mask} 的接口若把 {@code password}/{@code token}
     * 这类字段落库，等于把凭据写进审计表（{@code HttpContext.paramMap} 记录的是原始参数，
     * 登录接口的明文口令就在其中），不应依赖使用者逐个补 {@code mask}。</p>
     *
     * @param apiLogAnno 接口日志注解
     * @param json       已序列化的 JSON 文本
     * @return 脱敏后的 JSON；文本为空或无需脱敏时原样返回
     */
    private String maskIfConfigured(ApiLog apiLogAnno, String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        // 字段名统一按 ROOT Locale 小写：默认 Locale 下（如土耳其语）"ID" 会变成 "ıd"，导致掩码静默失效
        Set<String> maskFields = Arrays.stream(apiLogAnno.mask())
                .map(field -> field.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!requiresMasking(json, maskFields)) {
            // 未配置 mask 且文本里没有任何凭据词：免去一次 JSON 解析 + 重序列化
            return json;
        }
        return applyMask(json, maskFields);
    }

    /**
     * 判断该段文本是否值得付出一次解析/重序列化的代价
     * <p>注解给了字段名时保持既有行为（直接脱敏）；仅靠内置凭据词时才做一次廉价的子串预筛，
     * 避免给所有 {@code @ApiLog} 请求都加上 JSON 解析开销。</p>
     */
    private static boolean requiresMasking(String json, Set<String> maskFields) {
        if (!maskFields.isEmpty()) {
            return true;
        }
        String lower = json.toLowerCase(Locale.ROOT);
        return DEFAULT_CREDENTIAL_SUFFIXES.stream().anyMatch(lower::contains);
    }

    /**
     * 字段是否需要脱敏
     * <p>两条规则：</p>
     * <ul>
     *   <li>注解 {@code mask} 给出的名字 —— <b>精确</b>匹配（大小写不敏感），与既有语义保持一致，
     *       避免使用者写了 {@code id} 这类短名后把 {@code orderId}/{@code userId} 一并抹掉</li>
     *   <li>内置凭据词 —— <b>后缀</b>匹配，使 {@code userPassword}/{@code clientSecret}/{@code csrfToken}
     *       这类组合命名同样被覆盖；词表只含较长的专用词，避免误伤</li>
     * </ul>
     */
    private static boolean isSensitiveKey(String key, Set<String> maskFields) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (maskFields.contains(lower)) {
            return true;
        }
        return DEFAULT_CREDENTIAL_SUFFIXES.stream().anyMatch(lower::endsWith);
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
        } catch (Throwable e) {
            // 不能回退原文：那等于把未脱敏的敏感数据落库，与配置 mask 的意图相反。宁可丢弃这段内容。
            // 捕获 Throwable：深嵌套 JSON 会让 JSON.parse 抛 StackOverflowError，同样必须兜住
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
                if (isSensitiveKey(key, maskFields)) {
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
        } catch (Throwable e) {
            // 捕获 Throwable：循环引用/深嵌套对象会让 fastjson2 抛 StackOverflowError
            log.error("请求参数序列化失败", e);
            return StrConst.EMPTY;
        }
    }

}
