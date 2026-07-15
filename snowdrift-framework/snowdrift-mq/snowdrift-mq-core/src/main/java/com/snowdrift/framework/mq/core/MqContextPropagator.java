package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.common.util.EncryptUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MQ 上下文传播器 — 在消息头中注入 / 提取 TTL 上下文
 * <p>
 * Producer 端：将当前 {@link SecurityContextHolder} 和 traceId 注入到消息头。
 * Consumer 端：从消息头恢复上下文，确保下游服务可获取原始请求链路的用户信息和链路追踪。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqContextPropagator {

    public static final String TRACE_ID_KEY = "traceId";

    /** 发送端：消息 Key */
    public static final String HEADER_MESSAGE_KEY = "x-snowdrift-message-key";

    /** 链路追踪 ID */
    public static final String HEADER_TRACE_ID = "x-snowdrift-trace-id";

    /** 用户 ID */
    public static final String HEADER_USER_ID = "x-snowdrift-user-id";

    /** 登录账号 */
    public static final String HEADER_USERNAME = "x-snowdrift-username";

    /** 租户 ID */
    public static final String HEADER_TENANT_ID = "x-snowdrift-tenant-id";

    /** 部门 ID */
    public static final String HEADER_DEPT_ID = "x-snowdrift-dept-id";

    /** 数据权限 */
    public static final String HEADER_DATA_SCOPE = "x-snowdrift-data-scope";

    /** 签名 */
    private static final String SIGNATURE_HEADER = "x-snowdrift-signature";

    private final MqProperties properties;

    public MqContextPropagator(MqProperties properties) {
        this.properties = properties;
    }

    /**
     * 发送前：将当前 TTL 上下文注入到消息头
     *
     * @param builder 消息构建器
     * @param <T>     消息体类型
     * @return 注入上下文后的 builder（链式调用）
     */
    public <T> MessageBuilder<T> inject(MessageBuilder<T> builder) {
        // 注入 TraceId
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isNotBlank(traceId)) {
            builder.setHeader(HEADER_TRACE_ID, traceId);
        }

        // 注入安全上下文（getContext() 永不为 null，返回空上下文而非 null）
        SecurityContext ctx = SecurityContextHolder.getContext();
        String userIdStr = null;
        String username = null;
        String tenantIdStr = null;
        if (ctx.getUserId() != null) {
            userIdStr = ctx.getUserId().toString();
            builder.setHeader(HEADER_USER_ID, userIdStr);
        }
        if (StringUtils.isNotBlank(ctx.getUsername())) {
            username = ctx.getUsername();
            builder.setHeader(HEADER_USERNAME, username);
        }
        if (ctx.getTenantId() != null) {
            tenantIdStr = ctx.getTenantId().toString();
            builder.setHeader(HEADER_TENANT_ID, tenantIdStr);
        }
        String deptIdStr = null;
        String dataScopeStr = null;
        if (ctx.getDeptId() != null) {
            deptIdStr = ctx.getDeptId().toString();
            builder.setHeader(HEADER_DEPT_ID, deptIdStr);
        }
        if (ctx.getDataScope() != null) {
            dataScopeStr = ctx.getDataScope().toString();
            builder.setHeader(HEADER_DATA_SCOPE, dataScopeStr);
        }

        // 计算签名
        if (Boolean.TRUE.equals(properties.getSign()) && StringUtils.isNotBlank(properties.getSignKey())) {
            String payload = buildSignPayload(traceId, userIdStr, username, tenantIdStr, deptIdStr, dataScopeStr);
            String signature = EncryptUtil.hmacSha256(payload, properties.getSignKey());
            builder.setHeader(SIGNATURE_HEADER, signature);
        }
        return builder;
    }

    /**
     * 消费前：从消息头恢复 TTL 上下文
     * <p>
     * 恢复 traceId 到 MDC，恢复 SecurityContext 到 SecurityContextHolder。
     * 仅当消息头中存在对应字段时才覆盖当前值。
     * 若开启了签名校验，签名不通过则拒绝恢复上下文。
     * </p>
     *
     * @param message 消息
     */
    public void restore(Message<?> message) {
        // 验证签名：签名不通过则拒绝恢复上下文，防止伪造身份
        if (Boolean.TRUE.equals(properties.getSign())) {
            if (!verifySignature(message)) {
                log.warn("MQ 消息签名校验不通过，上下文已丢弃");
                clear();
                return;
            }
        }
        // 恢复 TraceId 到 MDC
        String traceId = message.getHeaders().get(HEADER_TRACE_ID, String.class);
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            // 上游未携带 traceId，生成新的
            MDC.put(TRACE_ID_KEY, java.util.UUID.randomUUID().toString());
        }

        // 恢复安全上下文
        String userIdStr = message.getHeaders().get(HEADER_USER_ID, String.class);
        String username = message.getHeaders().get(HEADER_USERNAME, String.class);
        String tenantIdStr = message.getHeaders().get(HEADER_TENANT_ID, String.class);
        String deptIdStr = message.getHeaders().get(HEADER_DEPT_ID, String.class);
        String dataScopeStr = message.getHeaders().get(HEADER_DATA_SCOPE, String.class);

        if (userIdStr != null || StringUtils.isNotBlank(username) || tenantIdStr != null
                || deptIdStr != null || dataScopeStr != null) {
            SecurityContext.SecurityContextBuilder builder = SecurityContext.builder();
            if (userIdStr != null) {
                try {
                    builder.userId(Long.parseLong(userIdStr));
                } catch (NumberFormatException e) {
                    log.debug("解析 userId 失败: {}", userIdStr);
                }
            }
            if (StringUtils.isNotBlank(username)) {
                builder.username(username);
            }
            if (tenantIdStr != null) {
                try {
                    builder.tenantId(Long.parseLong(tenantIdStr));
                } catch (NumberFormatException e) {
                    log.debug("解析 tenantId 失败: {}", tenantIdStr);
                }
            }
            if (deptIdStr != null) {
                try {
                    builder.deptId(Long.parseLong(deptIdStr));
                } catch (NumberFormatException e) {
                    log.debug("解析 deptId 失败: {}", deptIdStr);
                }
            }
            if (dataScopeStr != null) {
                try {
                    builder.dataScope(Integer.parseInt(dataScopeStr));
                } catch (NumberFormatException e) {
                    log.debug("解析 dataScope 失败: {}", dataScopeStr);
                }
            }
            SecurityContextHolder.setContext(builder.build());
        }
    }

    /**
     * 消费完成后：清除上下文
     * <p>
     * 避免上下文残留到下次消费（特别是在线程池模式下）。
     * </p>
     */
    public void clear() {
        SecurityContextHolder.clear();
        MDC.remove(TRACE_ID_KEY);
    }

    // ========== 签名相关 ==========

    /**
     * 验证消息签名
     *
     * @param message 消息
     * @return true 签名通过，false 签名不通过或缺少签名
     */
    private boolean verifySignature(Message<?> message) {
        String expected = message.getHeaders().get(SIGNATURE_HEADER, String.class);
        if (StringUtils.isBlank(expected)) {
            log.warn("MQ 消息缺少签名 header，上下文已丢弃");
            return false;
        }
        String payload = buildSignPayloadFromMessage(message);
        String actual = EncryptUtil.hmacSha256(payload, properties.getSignKey());
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        return true;
    }

    /**
     * 构建签名规范字符串（inject 端，从原始值构建）
     * <p>所有上下文字段按固定顺序拼接，空值保留 key 占位，防止通过"省略 header"绕过签名</p>
     */
    private String buildSignPayload(String traceId, String userId, String username, String tenantId,
                                     String deptId, String dataScope) {
        return "traceId=" + (traceId != null ? traceId : "")
                + "&userId=" + (userId != null ? userId : "")
                + "&username=" + (username != null ? username : "")
                + "&tenantId=" + (tenantId != null ? tenantId : "")
                + "&deptId=" + (deptId != null ? deptId : "")
                + "&dataScope=" + (dataScope != null ? dataScope : "");
    }

    /**
     * 构建签名规范字符串（restore 端，从 Message headers 构建）
     */
    private String buildSignPayloadFromMessage(Message<?> message) {
        return buildSignPayload(
                message.getHeaders().get(HEADER_TRACE_ID, String.class),
                message.getHeaders().get(HEADER_USER_ID, String.class),
                message.getHeaders().get(HEADER_USERNAME, String.class),
                message.getHeaders().get(HEADER_TENANT_ID, String.class),
                message.getHeaders().get(HEADER_DEPT_ID, String.class),
                message.getHeaders().get(HEADER_DATA_SCOPE, String.class));
    }

}
