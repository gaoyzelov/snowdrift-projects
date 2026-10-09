package com.snowdrift.framework.base.enums;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 通用枚举接口单元测试
 * @since 1.0.0
 */
class IEnumTest {

    /** 供测试使用的枚举：一个正常项、一个 code 为 null 的项，用于验证查找语义 */
    enum Color implements IEnum<Integer> {
        RED(1, "红"),
        GREEN(2, "绿"),
        UNKNOWN(null, "未知");

        private final Integer code;
        private final String note;

        Color(Integer code, String note) {
            this.code = code;
            this.note = note;
        }

        @Override
        public Integer getCode() {
            return code;
        }

        @Override
        public String getNote() {
            return note;
        }
    }

    @Test
    @DisplayName("getByCode 命中返回 Optional，未命中返回 empty")
    void getByCode() {
        assertThat(IEnum.getByCode(Color.class, 1)).contains(Color.RED);
        assertThat(IEnum.getByCode(Color.class, 2)).contains(Color.GREEN);
        assertThat(IEnum.getByCode(Color.class, 99)).isEmpty();
    }

    @Test
    @DisplayName("getByCode 用 Objects.equals 比较：code 为 null 时命中第一个 code 为 null 的枚举项")
    void getByCodeWithNullCode() {
        assertThat(Color.UNKNOWN.getCode()).isNull();

        // 注意：传入 null 不是「查不到」，而是精确匹配 code == null 的枚举项；
        // 枚举中若不存在 code 为 null 的项，才返回 empty
        assertThat(IEnum.getByCode(Color.class, null)).contains(Color.UNKNOWN);
    }

    @Test
    @DisplayName("getByNote 命中返回 Optional，未命中返回 empty")
    void getByNote() {
        assertThat(IEnum.getByNote(Color.class, "红")).contains(Color.RED);
        assertThat(IEnum.getByNote(Color.class, "不存在")).isEmpty();
        assertThat(IEnum.getByNote(Color.class, null)).isEmpty();
    }

    @Test
    @DisplayName("枚举类为 null 或非枚举类型时抛 BizException")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void invalidEnumClass() {
        assertThatThrownBy(() -> IEnum.getByCode(null, 1))
                .isInstanceOf(BizException.class)
                .hasMessage("必须传入枚举类型");

        assertThatThrownBy(() -> IEnum.getByNote(null, "红"))
                .isInstanceOf(BizException.class)
                .hasMessage("必须传入枚举类型");

        // 泛型上界要求 T 实现 IEnum，非枚举类只能在原始类型/反射场景下传入，
        // 方法内的 isEnum() 校验正是为这类调用兜底
        Class rawClass = String.class;
        assertThatThrownBy(() -> IEnum.getByCode(rawClass, 1))
                .isInstanceOf(BizException.class)
                .hasMessage("必须传入枚举类型");
        assertThatThrownBy(() -> IEnum.getByNote(rawClass, "红"))
                .isInstanceOf(BizException.class)
                .hasMessage("必须传入枚举类型");
    }

    @Test
    @DisplayName("getByCode/getByNote 均不返回 null")
    void neverReturnsNull() {
        Optional<Color> byCode = IEnum.getByCode(Color.class, 42);
        Optional<Color> byNote = IEnum.getByNote(Color.class, "x");

        assertThat(byCode).isNotNull().isEmpty();
        assertThat(byNote).isNotNull().isEmpty();
    }
}
