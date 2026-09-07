package com.snowdrift.framework.mq.context;

import com.snowdrift.framework.base.util.EncryptUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * MQ 上下文传播器 — 在消息头中注入 / 提取 TTL 上下文
 * <p>
 * Producer 端：将当前 {@link SecurityContextHolder} 和 traceId 注入到 broker 无关的 {@code Map<String,String>} 消息头。
 * Consumer 端：从消息头恢复上下文，确保下游服务可获取原始请求链路的用户信息和链路追踪。
 * 与具体 broker 解耦（Kafka / RabbitMQ / RocketMQ 的头均由实现模块与 Map 互转）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqContextPropagator {

    public static final String TRACE_ID_KEY = "traceId";

    /** 发送端：消息 Key（仅内省用途，broker 分区键由实现模块另行映射） */
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

    /** 签名 */
    private static final String SIGNATURE_HEADER = "x-snowdrift-signature";

    private final MqProperties properties;

    public MqContextPropagator(MqProperties properties) {
        this.properties = properties;
        // 签名开关与密钥联动校验：开启签名却未配置密钥，将导致生产者不签名 / 消费者全量拒收的灾难性不对称，启动即失败
        if (Boolean.TRUE.equals(properties.getSign()) && StringUtils.isBlank(properties.getSignKey())) {
            throw new IllegalStateException(
                    "snowdrift.mq.sign=true 时必须配置 snowdrift.mq.sign-key，否则无法计算/校验消息签名");
        }
    }

    /**
     * 发送前：将当前 TTL 上下文注入到消息头 Map
     *
     * @param headers 待填充的消息头（就地写入并返回，便于链式）
     * @return 填充后的 headers
     */
    public Map<String, String> inject(Map<String, String> headers) {
        // 注入 TraceId
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isNotBlank(traceId)) {
            headers.put(HEADER_TRACE_ID, traceId);
        }

        // 注入安全上下文（非 HTTP 线程无上下文时降级为空上下文）
        SecurityContext ctx;
        try {
            ctx = SecurityContextHolder.getContext();
        } catch (Exception e) {
            ctx = SecurityContext.builder().build();
        }
        String userIdStr = null;
        String username = null;
        String tenantIdStr = null;
        String deptIdStr = null;
        if (ctx.getUserId() != null) {
            userIdStr = ctx.getUserId().toString();
            headers.put(HEADER_USER_ID, userIdStr);
        }
        if (StringUtils.isNotBlank(ctx.getUsername())) {
            username = ctx.getUsername();
            headers.put(HEADER_USERNAME, username);
        }
        if (ctx.getTenantId() != null) {
            tenantIdStr = ctx.getTenantId().toString();
            headers.put(HEADER_TENANT_ID, tenantIdStr);
        }
        if (ctx.getDeptId() != null) {
            deptIdStr = ctx.getDeptId().toString();
            headers.put(HEADER_DEPT_ID, deptIdStr);
        }

        // 计算签名
        if (Boolean.TRUE.equals(properties.getSign()) && StringUtils.isNotBlank(properties.getSignKey())) {
            String payload = buildSignPayload(traceId, userIdStr, username, tenantIdStr, deptIdStr);
            headers.put(SIGNATURE_HEADER, EncryptUtil.hmacSha256(payload, properties.getSignKey()));
        }
        return headers;
    }

    /**
     * 消费前：从消息头 Map 恢复 TTL 上下文
     * <p>
     * 恢复 traceId 到 MDC，恢复 SecurityContext 到 SecurityContextHolder。
     * 若开启了签名校验，签名不通过则拒绝恢复上下文。
     * 无论消息是否携带身份头，都会用（可能为空的）上下文显式覆盖当前线程状态，避免线程残留。
     * </p>
     *
     * @param headers 消息头 Map
     */
    public void restore(Map<String, String> headers) {
        // 验证签名：签名不通过则拒绝消费消息，防止伪造身份
        if (Boolean.TRUE.equals(properties.getSign()) && !verifySignature(headers)) {
            log.warn("MQ 消息签名校验不通过，拒绝消费");
            clear();
            throw new MqException("消息签名校验失败，已拒绝消费该消息");
        }
        // 恢复 TraceId 到 MDC
        String traceId = headers.get(HEADER_TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            // 上游未携带 traceId，生成新的
            MDC.put(TRACE_ID_KEY, java.util.UUID.randomUUID().toString());
        }

        // 恢复安全上下文
        String userIdStr = headers.get(HEADER_USER_ID);
        String username = headers.get(HEADER_USERNAME);
        String tenantIdStr = headers.get(HEADER_TENANT_ID);
        String deptIdStr = headers.get(HEADER_DEPT_ID);

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
        SecurityContextHolder.setContext(builder.build());
    }

    /**
     * 消费完成后：清除上下文，避免上下文残留到下次消费（线程池/容器线程复用场景）
     */
    public void clear() {
        SecurityContextHolder.clear();
        MDC.remove(TRACE_ID_KEY);
    }

    // ========== 签名相关 ==========

    /**
     * 验证消息签名
     *
     * @param headers 消息头 Map
     * @return true 签名通过，false 签名不通过或缺少签名
     */
    private boolean verifySignature(Map<String, String> headers) {
        String expected = headers.get(SIGNATURE_HEADER);
        if (StringUtils.isBlank(expected)) {
            log.warn("MQ 消息缺少签名 header，上下文已丢弃");
            return false;
        }
        String payload = buildSignPayload(
                headers.get(HEADER_TRACE_ID),
                headers.get(HEADER_USER_ID),
                headers.get(HEADER_USERNAME),
                headers.get(HEADER_TENANT_ID),
                headers.get(HEADER_DEPT_ID));
        String actual = EncryptUtil.hmacSha256(payload, properties.getSignKey());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建签名规范字符串
     * <p>所有上下文字段按固定顺序拼接，空值保留 key 占位，防止通过"省略 header"绕过签名</p>
     */
    private String buildSignPayload(String traceId, String userId, String username, String tenantId, String deptId) {
        return "traceId=" + (traceId != null ? traceId : "")
                + "&userId=" + (userId != null ? userId : "")
                + "&username=" + (username != null ? username : "")
                + "&tenantId=" + (tenantId != null ? tenantId : "")
                + "&deptId=" + (deptId != null ? deptId : "");
    }

}
