package com.snowdrift.framework.base.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowflakeUtilDefaultInstanceWarningTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 无参 getInstance() 使用默认 (0,0) 标识，多副本部署必然产生重复 ID，
 *              必须在运行时留下可观测的 WARN，而不是只写在 Javadoc 里
 * @since 1.0.0
 */
@ExtendWith(OutputCaptureExtension.class)
class SnowflakeUtilDefaultInstanceWarningTest {

    @Test
    @DisplayName("无参 getInstance()：给出 WARN，指明默认标识会导致多节点重复 ID")
    void defaultInstanceWarnsAboutDuplicateIds(CapturedOutput output) {
        assertThat(SnowflakeUtil.getInstance()).isNotNull();

        assertThat(output).contains("SnowflakeUtil");
        assertThat(output).contains("workerId");
        assertThat(output).contains("重复");
    }

    @Test
    @DisplayName("显式指定标识时不告警（该重载是分布式下的正确用法）")
    void explicitIdsDoNotWarn(CapturedOutput output) {
        assertThat(SnowflakeUtil.getInstance(7L, 3L)).isNotNull();

        assertThat(output).doesNotContain("必须显式指定");
    }
}
