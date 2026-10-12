package com.snowdrift.framework.orm.core.scope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.snowdrift.framework.base.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 数据权限范围枚举黑盒测试：of(code) 的正常映射、null 与未知编码的 fail-closed 回退（NONE）、
 * 六种范围的 code/note 契约、IEnum 反查以及 JSON 序列化注解
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

    @Test
    @DisplayName("恰好 6 种数据范围，code 互不相同，且全部实现 IEnum")
    void sixDistinctRanges() {
        assertThat(DataScopeEnum.values()).hasSize(6);
        assertThat(DataScopeEnum.values()).extracting(DataScopeEnum::getCode).doesNotHaveDuplicates();
        assertThat(DataScopeEnum.values()).allSatisfy(e -> assertThat(e).isInstanceOf(IEnum.class));
    }

    @Test
    @DisplayName("note 契约完整：无权限 + 五种业务范围")
    void notesContract() {
        assertThat(DataScopeEnum.NONE.getNote()).isEqualTo("无权限");
        assertThat(DataScopeEnum.ALL.getNote()).isEqualTo("全部");
        assertThat(DataScopeEnum.DEPT.getNote()).isEqualTo("本部门");
        assertThat(DataScopeEnum.DEPT_AND_SUB.getNote()).isEqualTo("本部门及子部门");
        assertThat(DataScopeEnum.SELF.getNote()).isEqualTo("仅自己");
        assertThat(DataScopeEnum.CUSTOM.getNote()).isEqualTo("自定义");
    }

    @Test
    @DisplayName("IEnum 反查：按 code / note 命中，未知值返回空 Optional")
    void lookupByCodeAndNote() {
        assertThat(IEnum.getByCode(DataScopeEnum.class, 3)).contains(DataScopeEnum.SELF);
        assertThat(IEnum.getByCode(DataScopeEnum.class, -1)).contains(DataScopeEnum.NONE);
        assertThat(IEnum.getByNote(DataScopeEnum.class, "仅自己")).contains(DataScopeEnum.SELF);
        assertThat(IEnum.getByCode(DataScopeEnum.class, 5)).isEmpty();
        assertThat(IEnum.getByNote(DataScopeEnum.class, "不存在")).isEmpty();
    }

    @Test
    @DisplayName("边界：负编码/整数极值/越界编码一律回退 NONE（fail-closed）")
    void extremeCodesFallBackToNone() {
        assertThat(DataScopeEnum.of(-2)).isEqualTo(DataScopeEnum.NONE);
        assertThat(DataScopeEnum.of(Integer.MIN_VALUE)).isEqualTo(DataScopeEnum.NONE);
        assertThat(DataScopeEnum.of(Integer.MAX_VALUE)).isEqualTo(DataScopeEnum.NONE);
        assertThat(DataScopeEnum.of(5)).isEqualTo(DataScopeEnum.NONE);
    }

    @Test
    @DisplayName("序列化契约：code 字段标注 @JsonValue、of(Integer) 标注 @JsonCreator")
    void jsonAnnotationsPresent() throws Exception {
        Field code = DataScopeEnum.class.getDeclaredField("code");
        assertThat(code.getAnnotation(JsonValue.class)).isNotNull();
        assertThat(DataScopeEnum.class.getMethod("of", Integer.class).getAnnotation(JsonCreator.class)).isNotNull();
    }
}
