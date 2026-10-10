package com.snowdrift.framework.orm.core.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 数据权限范围枚举黑盒测试：of(code) 的正常映射、null 与未知编码的 fail-closed 回退（NONE）
 * @since 1.0.0
 */
class DataScopeEnumTest {

    @Test
    @DisplayName("of：已知编码映射到对应枚举")
    void mapsKnownCodes() {
        assertThat(DataScopeEnum.of(-1)).isEqualTo(DataScopeEnum.NONE);
        assertThat(DataScopeEnum.of(0)).isEqualTo(DataScopeEnum.ALL);
        assertThat(DataScopeEnum.of(1)).isEqualTo(DataScopeEnum.DEPT);
        assertThat(DataScopeEnum.of(2)).isEqualTo(DataScopeEnum.DEPT_AND_SUB);
        assertThat(DataScopeEnum.of(3)).isEqualTo(DataScopeEnum.SELF);
        assertThat(DataScopeEnum.of(4)).isEqualTo(DataScopeEnum.CUSTOM);
    }

    @Test
    @DisplayName("边界：null 编码回退到 NONE，避免扩大数据可见范围")
    void nullCodeFallsBackToNone() {
        assertThat(DataScopeEnum.of(null)).isEqualTo(DataScopeEnum.NONE);
    }

    @Test
    @DisplayName("边界：未知编码回退到 NONE")
    void unknownCodeFallsBackToNone() {
        assertThat(DataScopeEnum.of(999)).isEqualTo(DataScopeEnum.NONE);
    }

    @Test
    @DisplayName("枚举 code/note 读取正确")
    void exposesCodeAndNote() {
        assertThat(DataScopeEnum.ALL.getCode()).isEqualTo(0);
        assertThat(DataScopeEnum.DEPT.getNote()).isEqualTo("本部门");
    }
}
