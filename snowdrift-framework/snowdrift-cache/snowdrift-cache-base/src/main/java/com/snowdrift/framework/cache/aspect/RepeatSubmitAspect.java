package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.annotation.RepeatSubmit;
import com.snowdrift.framework.cache.util.SpELUtil;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.framework.base.util.EncryptUtil;
import com.snowdrift.framework.base.util.IpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 重复提交防护 AOP 切面
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Order(1)
public class RepeatSubmitAspect {

    private static final String PREFIX = "repeat:submit:";

    private final ICacheService cacheService;

    public RepeatSubmitAspect(ICacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        if (cacheService == null) {
            throw new IllegalStateException("@RepeatSubmit 未配置 ICacheService，已拒绝执行业务方法；"
                    + "请引入 snowdrift-cache-caffeine、snowdrift-cache-redis 或注册自定义缓存实现");
        }
        AssertUtil.predicate(() -> repeatSubmit.interval() > 0, "防重间隔必须大于 0");
        // 非 Web 环境直接放行
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            log.debug("非 Web 环境，跳过防重校验: method={}", joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        String key = generateKey(repeatSubmit, servletRequestAttributes.getRequest(), joinPoint);
        if (key == null) {
            // key 无法可靠生成，放行本次请求（原因已在 generateKey 内记日志）
            return joinPoint.proceed();
        }

        Duration ttl = Duration.of(repeatSubmit.interval(), repeatSubmit.timeUnit().toChronoUnit());
        log.debug("重复提交检查: key={}, interval={}", key, ttl);

        // 写入本请求唯一 nonce 作为幂等标记值，用于失败清理时区分是否仍为本请求写入的标记
        String nonce = UUID.randomUUID().toString();
        // 尝试写入标记，写入失败表示已存在（重复提交）
        boolean success = cacheService.putIfAbsent(key, nonce, ttl);
        if (!success) {
            log.warn("检测到重复提交: key={}", key);
            throw new BizException(repeatSubmit.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 执行失败时删除标记，允许用户修正后重新提交。
            clearMarker(key, nonce);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw e;
        }
    }

    /**
     * 清理本请求写入的幂等标记
     * <p>仅当缓存中仍为本请求写入的 nonce 时才删除，避免误删「本请求标记已过期、被并发请求重新写入」
     * 的新标记。读-删之间存在极小竞态窗口，但 {@link ICacheService} 未提供原子 compare-and-remove
     * 原语，此处接受该残余竞态。删除失败仅记日志，不覆盖原始业务异常。</p>
     *
     * @param key   防重 key
     * @param nonce 本请求写入标记时的随机值
     */
    private void clearMarker(String key, String nonce) {
        try {
            String current = cacheService.get(key, String.class);
            if (nonce.equals(current)) {
                cacheService.delete(key);
            }
        } catch (Exception deleteEx) {
            log.error("重复提交标记删除失败: key={}", key, deleteEx);
        }
    }

    /**
     * 生成防重 key：{@code repeat:submit:ip:<客户端IP>:<请求URI>:<作用域>}
     *
     * @param repeatSubmit 注解
     * @param request      当前请求
     * @param joinPoint    切点
     * @return 防重 key；身份或作用域无法可靠生成时返回 {@code null}，由调用方放行
     */
    private String generateKey(RepeatSubmit repeatSubmit, HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        String ip = IpUtil.getIp(request);
        if (StringUtils.isBlank(ip) || StringUtils.equals(ip, StrConst.UNKNOWN)) {
            // 身份不可识别时不能用 unknown 兜底, 那会让所有这类客户端共用同一个 key
            log.warn("客户端 IP 无法识别，跳过防重校验: uri={}", request.getRequestURI());
            return null;
        }

        // URI / 作用域中的分隔符转义
        String scope = resolveScope(repeatSubmit, joinPoint);
        if (scope == null) {
            return null;
        }
        // IP 段同样必须转义：IPv6 规范化串本身含 ':'（如 2001:db8::1），
        // 裸拼会让「身份段」与「接口段」的边界不可分辨，破坏 key 的段边界不变量。
        return PREFIX + "ip:" + escapeDelimiter(ip)
                + StrConst.COLON + escapeDelimiter(StringUtils.defaultString(request.getRequestURI()))
                + StrConst.COLON + escapeDelimiter(scope);
    }

    /**
     * 解析 key 的作用域
     *
     * @param repeatSubmit 注解
     * @param joinPoint    切点
     * @return 作用域；无法可靠生成时返回 {@code null}（由调用方放行）
     */
    private String resolveScope(RepeatSubmit repeatSubmit, ProceedingJoinPoint joinPoint) {
        String spElKey = repeatSubmit.key();
        if (StringUtils.isBlank(spElKey)) {
            // 默认作用域：业务参数指纹
            return buildParamsFingerprint(joinPoint);
        }
        // 显式指定了 key：SpEL 结果作为作用域，外层仍带「身份 + 接口」，避免不同用户 /
        // 不同接口算出相同 SpEL 结果时互相误伤
        try {
            String scope = SpELUtil.parseExpression(spElKey, joinPoint);
            if (StringUtils.isBlank(scope)) {
                log.error("防重提交 SpEL 解析结果为空，本次跳过防重校验: key={}", spElKey);
                return null;
            }
            return scope;
        } catch (Exception e) {
            log.error("防重提交 SpEL 解析失败，本次跳过防重校验: key={}", spElKey, e);
            return null;
        }
    }

    /**
     * 构造业务参数指纹（MD5）
     *
     * @param joinPoint 切点
     * @return 参数指纹；序列化失败时返回 {@code null}（由调用方放行）
     */
    private String buildParamsFingerprint(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        List<Object> businessArgs = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(args)) {
            for (Object arg : args) {
                if (Objects.isNull(arg) || isInfrastructureArg(arg)) {
                    continue;
                }
                businessArgs.add(arg instanceof MultipartFile file
                        ? "file:" + file.getOriginalFilename() + ":" + file.getSize()
                        : arg);
            }
        }
        try {
            String json = JSON.toJSONString(businessArgs,
                    JSONWriter.Feature.FieldBased, JSONWriter.Feature.SortMapEntriesByKeys);
            return EncryptUtil.md5(json);
        } catch (Exception e) {
            log.error("防重提交参数序列化失败，本次跳过防重校验，建议为该接口显式指定 @RepeatSubmit(key = \"...\")", e);
            return null;
        }
    }

    /**
     * 转义 key 片段中的分隔符
     * <p>不转义时 {@code uri="/a:b" + scope="c"} 与 {@code uri="/a" + scope="b:c"} 会拼出同一个 key。
     * 反斜杠须先转义，保证结果可逆（与 {@code SnowdriftKeyGenerator} 同一规则）。</p>
     *
     * @param fragment key 片段
     * @return 转义后的片段
     */
    private String escapeDelimiter(String fragment) {
        return fragment.replace("\\", "\\\\").replace(StrConst.COLON, "\\:");
    }

    /**
     * 判断是否为请求绑定类参数（不参与参数指纹）
     *
     * @param arg 方法入参
     * @return 属于请求绑定类参数时返回 true
     */
    private boolean isInfrastructureArg(Object arg) {
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof Errors;
    }
}
