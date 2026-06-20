package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * MQ 上下文传播器 — 在消息头中注入 / 提取 TTL 上下文
 * <p>
 * Producer 端：将当前 {@link SecurityContextHolder} 和 traceId 注入到消息头。
 * Consumer 端：从消息头恢复上下文，确保下游服务可获取原始请求链路的用户信息和链路追踪。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public final class MqContextPropagator {

    private static final String TRACE_ID_KEY = "traceId";

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

    private MqContextPropagator() {
    }

    /**
     * 发送前：将当前 TTL 上下文注入到消息头
     *
     * @param builder 消息构建器
     * @param <T>     消息体类型
     * @return 注入上下文后的 builder（链式调用）
     */
    public static <T> MessageBuilder<T> inject(MessageBuilder<T> builder) {
        // 注入 TraceId
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isNotBlank(traceId)) {
            builder.setHeader(HEADER_TRACE_ID, traceId);
        }

        // 注入安全上下文（getContext() 永不为 null，返回空上下文而非 null）
        SecurityContext ctx = SecurityContextHolder.getContext();
        if (ctx.getUserId() != null) {
            builder.setHeader(HEADER_USER_ID, ctx.getUserId().toString());
        }
        if (StringUtils.isNotBlank(ctx.getUsername())) {
            builder.setHeader(HEADER_USERNAME, ctx.getUsername());
        }
        if (ctx.getTenantId() != null) {
            builder.setHeader(HEADER_TENANT_ID, ctx.getTenantId().toString());
        }
        return builder;
    }

    /**
     * 消费前：从消息头恢复 TTL 上下文
     * <p>
     * 恢复 traceId 到 MDC，恢复 SecurityContext 到 SecurityContextHolder。
     * 仅当消息头中存在对应字段时才覆盖当前值。
     * </p>
     *
     * @param message 消息
     */
    public static void restore(Message<?> message) {
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

        if (userIdStr != null || StringUtils.isNotBlank(username) || tenantIdStr != null) {
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
            SecurityContextHolder.setContext(builder.build());
        }
    }

    /**
     * 消费完成后：清除上下文
     * <p>
     * 避免上下文残留到下次消费（特别是在线程池模式下）。
     * </p>
     */
    public static void clear() {
        SecurityContextHolder.clear();
        MDC.remove(TRACE_ID_KEY);
    }

}
