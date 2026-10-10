package com.snowdrift.framework.rpc.dubbo.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RpcContextConstantsTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 锁定 Dubbo attachment 线路协议 key：这些常量是 Consumer/Provider 双方约定，变更会破坏跨服务上下文传播
 * @since 1.0.0
 */
class RpcContextConstantsTest {

    @Test
    @DisplayName("attachment key 常量值保持稳定（线路协议契约）")
    void attachmentKeysAreStable() {
        assertThat(RpcContextConstants.TRACE_ID).isEqualTo("x-snowdrift-trace-id");
        assertThat(RpcContextConstants.SECURITY_CONTEXT).isEqualTo("x-snowdrift-security-context");
        assertThat(RpcContextConstants.CONTEXT_ERROR).isEqualTo("x-snowdrift-context-error");
    }

    @Test
    @DisplayName("三个 key 互不相同，避免附件覆盖")
    void keysAreDistinct() {
        assertThat(RpcContextConstants.TRACE_ID)
                .isNotEqualTo(RpcContextConstants.SECURITY_CONTEXT)
                .isNotEqualTo(RpcContextConstants.CONTEXT_ERROR);
        assertThat(RpcContextConstants.SECURITY_CONTEXT).isNotEqualTo(RpcContextConstants.CONTEXT_ERROR);
    }
}
