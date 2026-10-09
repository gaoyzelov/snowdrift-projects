package com.snowdrift.framework.log.enums;

import com.snowdrift.framework.base.enums.IEnum;
import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BizTypeEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 业务类型枚举单元测试
 * @since 1.0.0
 */
class BizTypeEnumTest {

    @Test
    @DisplayName("编码与说明稳定（下游按数值对接）")
    void codings() {
        assertThat(BizTypeEnum.INSERT.getCode()).isZero();
        assertThat(BizTypeEnum.UPDATE.getCode()).isEqualTo(1);
        assertThat(BizTypeEnum.DELETE.getCode()).isEqualTo(2);
        assertThat(BizTypeEnum.SELECT.getCode()).isEqualTo(3);
        assertThat(BizTypeEnum.EXPORT.getCode()).isEqualTo(4);
        assertThat(BizTypeEnum.IMPORT.getCode()).isEqualTo(5);
        assertThat(BizTypeEnum.OTHER.getCode()).isEqualTo(9);

        assertThat(BizTypeEnum.INSERT.getNote()).isEqualTo("新增");
        assertThat(BizTypeEnum.OTHER.getNote()).isEqualTo("其他");
    }

    @ParameterizedTest
    @EnumSource(BizTypeEnum.class)
    @DisplayName("实现 IEnum：编码非空、说明非空，且可被 IEnum 反查")
    void implementsIEnum(BizTypeEnum type) {
        assertThat(type.getCode()).isNotNull();
        assertThat(type.getNote()).isNotBlank();
        assertThat(IEnum.getByCode(BizTypeEnum.class, type.getCode())).contains(type);
        assertThat(IEnum.getByNote(BizTypeEnum.class, type.getNote())).contains(type);
    }

    @Test
    @DisplayName("of：按编码解析（Jackson @JsonCreator 入口）")
    void ofByCode() {
        assertThat(BizTypeEnum.of(0)).isEqualTo(BizTypeEnum.INSERT);
        assertThat(BizTypeEnum.of(9)).isEqualTo(BizTypeEnum.OTHER);
    }

    @Test
    @DisplayName("of：null 返回 null，未知编码抛 BizException")
    void ofEdgeCases() {
        assertThat(BizTypeEnum.of(null)).isNull();

        assertThatThrownBy(() -> BizTypeEnum.of(99))
                .isInstanceOf(BizException.class)
                .hasMessage("业务类型有误: 99");
    }

    @Test
    @DisplayName("全部编码唯一")
    void codesAreUnique() {
        assertThat(BizTypeEnum.values()).extracting(BizTypeEnum::getCode).doesNotHaveDuplicates();
    }
}
