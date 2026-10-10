package com.snowdrift.framework.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OssConstTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 锁定批量操作分区大小常量：该值对齐各云 SDK 单次批量删除请求上限，变更会影响批量删除正确性
 * @since 1.0.0
 */
class OssConstTest {

    @Test
    @DisplayName("BATCH_PARTITION_SIZE = 1000（SDK 单次批量上限）")
    void batchPartitionSize() {
        assertThat(OssConst.BATCH_PARTITION_SIZE).isEqualTo(1000);
    }
}
