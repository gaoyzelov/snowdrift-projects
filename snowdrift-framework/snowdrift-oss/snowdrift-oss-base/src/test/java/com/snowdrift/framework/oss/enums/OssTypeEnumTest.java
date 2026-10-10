package com.snowdrift.framework.oss.enums;

import com.snowdrift.framework.oss.exception.OssException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OssTypeEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 存储类型枚举黑盒测试：锁定 code（配置线路值）、getByCode 命中/未命中、from 未命中抛 OssException
 * @since 1.0.0
 */
class OssTypeEnumTest {

    @Test
    @DisplayName("五种类型 code/note 契约稳定")
    void codeContract() {
        assertThat(OssTypeEnum.MINIO.getCode()).isEqualTo("minio");
        assertThat(OssTypeEnum.ALIYUN.getCode()).isEqualTo("aliyun");
        assertThat(OssTypeEnum.QINIU.getCode()).isEqualTo("qiniu");
        assertThat(OssTypeEnum.TENCENT.getCode()).isEqualTo("tencent");
        assertThat(OssTypeEnum.LOCAL.getCode()).isEqualTo("local");
        assertThat(OssTypeEnum.values()).hasSize(5);
    }

    @Test
    @DisplayName("getByCode：命中返回 Optional，未命中/ null 返回空")
    void getByCode() {
        assertThat(OssTypeEnum.getByCode("minio")).contains(OssTypeEnum.MINIO);
        assertThat(OssTypeEnum.getByCode("unknown")).isEmpty();
        assertThat(OssTypeEnum.getByCode(null)).isEmpty();
    }

    @Test
    @DisplayName("from：命中返回枚举，未命中抛 OssException 并带上非法值")
    void fromThrowsOnUnknown() {
        assertThat(OssTypeEnum.from("local")).isEqualTo(OssTypeEnum.LOCAL);
        assertThatThrownBy(() -> OssTypeEnum.from("s3"))
                .isInstanceOf(OssException.class)
                .hasMessageContaining("s3");
    }
}
