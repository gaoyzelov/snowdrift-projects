package com.snowdrift.framework.orm.mp;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CryptoKeyHolderTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description AES 密钥静态持有器黑盒测试：设置/读取、可用性判定与未配置时的 fail-closed 抛错
 * @since 1.0.0
 */
class CryptoKeyHolderTest {

    @AfterEach
    void reset() {
        // 复位静态状态，避免污染其它测试类
        CryptoKeyHolder.setKey(null);
    }

    @Test
    @DisplayName("设置后可读取且标记为可用")
    void setAndGet() {
        CryptoKeyHolder.setKey("0123456789abcdef");

        assertThat(CryptoKeyHolder.isKeyAvailable()).isTrue();
        assertThat(CryptoKeyHolder.getKey()).isEqualTo("0123456789abcdef");
    }

    @Test
    @DisplayName("未设置密钥时 getKey 抛 BizException（fail-closed），isKeyAvailable 为 false")
    void getKeyThrowsWhenUnset() {
        CryptoKeyHolder.setKey(null);

        assertThat(CryptoKeyHolder.isKeyAvailable()).isFalse();
        assertThatThrownBy(CryptoKeyHolder::getKey)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密钥未配置");
    }

    @Test
    @DisplayName("边界：空白密钥视为不可用，getKey 抛错")
    void blankKeyTreatedAsUnavailable() {
        CryptoKeyHolder.setKey("   ");

        assertThat(CryptoKeyHolder.isKeyAvailable()).isFalse();
        assertThatThrownBy(CryptoKeyHolder::getKey).isInstanceOf(BizException.class);
    }
}
