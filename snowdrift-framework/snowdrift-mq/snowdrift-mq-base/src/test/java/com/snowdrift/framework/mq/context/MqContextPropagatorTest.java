package com.snowdrift.framework.mq.context;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MqContextPropagatorTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 上下文传播器单元测试：注入/恢复、签名开关与密钥联动校验、签名验证与拒绝、异常兜底与清理
 * @since 1.0.0
 */
class MqContextPropagatorTest {

    private static final String SIGNATURE_HEADER = "x-snowdrift-signature";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        MDC.clear();
    }

    private static MqProperties props(boolean sign, String signKey) {
        MqProperties properties = new MqProperties();
        properties.setSign(sign);
        properties.setSignKey(signKey);
        return properties;
    }

    @Test
    @DisplayName("无上下文且无 traceId 时注入不写任何身份头")
    void injectWithNoContextProducesEmptyHeaders() {
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));

        Map<String, String> headers = propagator.inject(new LinkedHashMap<>());

        assertThat(headers).isEmpty();
    }

    @Test
    @DisplayName("注入 traceId 与安全上下文各字段")
    void injectTraceIdAndSecurityContext() {
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-abc");
        SecurityContextHolder.setContext(SecurityContext.builder()
                .userId(1L).username("tom").tenantId(2L).deptId(3L).build());
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));

        Map<String, String> headers = propagator.inject(new LinkedHashMap<>());

        assertThat(headers)
                .containsEntry(MqContextPropagator.HEADER_TRACE_ID, "trace-abc")
                .containsEntry(MqContextPropagator.HEADER_USER_ID, "1")
                .containsEntry(MqContextPropagator.HEADER_USERNAME, "tom")
                .containsEntry(MqContextPropagator.HEADER_TENANT_ID, "2")
                .containsEntry(MqContextPropagator.HEADER_DEPT_ID, "3");
    }

    @Test
    @DisplayName("inject/restore 往返：消费端还原 traceId 与身份上下文")
    void restoreRoundTrip() {
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-xyz");
        SecurityContextHolder.setContext(SecurityContext.builder()
                .userId(10L).username("jerry").tenantId(20L).deptId(30L).build());
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));
        Map<String, String> headers = propagator.inject(new LinkedHashMap<>());

        // 模拟消费端：清空后从头恢复
        SecurityContextHolder.clear();
        MDC.clear();
        propagator.restore(headers);

        assertThat(MDC.get(MqContextPropagator.TRACE_ID_KEY)).isEqualTo("trace-xyz");
        SecurityContext restored = SecurityContextHolder.getContext();
        assertThat(restored.getUserId()).isEqualTo(10L);
        assertThat(restored.getUsername()).isEqualTo("jerry");
        assertThat(restored.getTenantId()).isEqualTo(20L);
        assertThat(restored.getDeptId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("上游未携带 traceId 时，restore 生成新的 traceId")
    void restoreGeneratesTraceIdWhenAbsent() {
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));

        propagator.restore(new HashMap<>());

        assertThat(MDC.get(MqContextPropagator.TRACE_ID_KEY)).isNotBlank();
    }

    @Test
    @DisplayName("身份数字字段非法时降级跳过，不抛异常")
    void restoreToleratesInvalidNumericIds() {
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));
        Map<String, String> headers = new HashMap<>();
        headers.put(MqContextPropagator.HEADER_USER_ID, "not-a-number");
        headers.put(MqContextPropagator.HEADER_TENANT_ID, "abc");
        headers.put(MqContextPropagator.HEADER_DEPT_ID, "");

        propagator.restore(headers);

        SecurityContext restored = SecurityContextHolder.getContext();
        assertThat(restored.getUserId()).isNull();
        assertThat(restored.getTenantId()).isNull();
        assertThat(restored.getDeptId()).isNull();
    }

    @Test
    @DisplayName("clear 清除 SecurityContext 与 traceId")
    void clearResetsThreadState() {
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "t");

        propagator.clear();

        assertThat(SecurityContextHolder.peekContext()).isNull();
        assertThat(MDC.get(MqContextPropagator.TRACE_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("sign=true 但未配置 signKey 时构造即失败，避免签名不对称灾难")
    void signEnabledWithoutKeyFailsFast() {
        assertThatThrownBy(() -> new MqContextPropagator(props(true, "  ")))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("sign-key");
    }

    @Test
    @DisplayName("开启签名：inject 写入签名头，restore 校验通过并恢复上下文")
    void signedRoundTripPasses() {
        MqContextPropagator propagator = new MqContextPropagator(props(true, "secret-key"));
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-1");
        SecurityContextHolder.setContext(SecurityContext.builder().userId(7L).username("sig").build());

        Map<String, String> headers = propagator.inject(new LinkedHashMap<>());
        assertThat(headers).containsKey(SIGNATURE_HEADER);

        SecurityContextHolder.clear();
        MDC.clear();
        propagator.restore(headers);

        assertThat(SecurityContextHolder.getContext().getUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("开启签名：身份头被篡改则校验失败，拒绝消费并清理上下文")
    void tamperedIdentityIsRejected() {
        MqContextPropagator propagator = new MqContextPropagator(props(true, "secret-key"));
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-1");
        SecurityContextHolder.setContext(SecurityContext.builder().userId(7L).username("sig").build());
        Map<String, String> headers = propagator.inject(new LinkedHashMap<>());

        // 攻击者篡改 userId，但没有密钥无法重算签名
        headers.put(MqContextPropagator.HEADER_USER_ID, "999");

        assertThatThrownBy(() -> propagator.restore(headers))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("签名校验失败");
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("开启签名：消息缺少签名头则拒绝消费")
    void missingSignatureIsRejected() {
        MqContextPropagator propagator = new MqContextPropagator(props(true, "secret-key"));
        Map<String, String> headers = new HashMap<>();
        headers.put(MqContextPropagator.HEADER_USER_ID, "1");

        assertThatThrownBy(() -> propagator.restore(headers))
                .isInstanceOf(MqException.class);
    }

    @Test
    @DisplayName("未开启签名（显式 opt-out）：即使头部含伪造签名也不做校验，正常恢复")
    void signDisabledSkipsVerification() {
        MqContextPropagator propagator = new MqContextPropagator(props(false, null));
        Map<String, String> headers = new HashMap<>();
        headers.put(MqContextPropagator.HEADER_USER_ID, "55");
        headers.put(SIGNATURE_HEADER, "forged");

        propagator.restore(headers);

        assertThat(SecurityContextHolder.getContext().getUserId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("默认配置即开启签名：未配置 sign-key 时构造失败，不 silently 信任未签名身份头")
    void signatureEnabledByDefault() {
        assertThat(new MqProperties().getSign()).isTrue();
        assertThatThrownBy(() -> new MqContextPropagator(new MqProperties()))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("sign-key");
    }

    @Test
    @DisplayName("默认配置下，携带伪造身份头的无签名消息被拒绝，不写入任何身份")
    void forgedIdentityRejectedByDefault() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        MqContextPropagator propagator = new MqContextPropagator(properties);

        Map<String, String> forged = new HashMap<>();
        forged.put(MqContextPropagator.HEADER_USER_ID, "1");
        forged.put(MqContextPropagator.HEADER_USERNAME, "attacker");
        forged.put(MqContextPropagator.HEADER_TENANT_ID, "999");
        forged.put(MqContextPropagator.HEADER_DEPT_ID, "1");
        forged.put(SIGNATURE_HEADER, "forged");

        assertThatThrownBy(() -> propagator.restore(forged))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("签名");
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }
}
