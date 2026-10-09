package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AssertUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 断言工具类单元测试
 * @since 1.0.0
 */
class AssertUtilTest {

    @Test
    @DisplayName("notNull：非 null 通过，null 抛业务异常并带自定义文案")
    void notNull() {
        assertThatCode(() -> AssertUtil.notNull(new Object(), "对象不能为空")).doesNotThrowAnyException();
        assertThatCode(() -> AssertUtil.notNull("", "对象不能为空")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.notNull(null, "对象不能为空"))
                .isInstanceOf(BizException.class)
                .hasMessage("对象不能为空");
    }

    @Test
    @DisplayName("notBlank：非空白通过，空白字符与 null 均失败")
    void notBlank() {
        assertThatCode(() -> AssertUtil.notBlank("a", "字符串不能为空")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.notBlank(null, "字符串不能为空"))
                .isInstanceOf(BizException.class).hasMessage("字符串不能为空");
        assertThatThrownBy(() -> AssertUtil.notBlank("", "字符串不能为空"))
                .isInstanceOf(BizException.class).hasMessage("字符串不能为空");
        assertThatThrownBy(() -> AssertUtil.notBlank("  \t\n", "字符串不能为空"))
                .isInstanceOf(BizException.class).hasMessage("字符串不能为空");
    }

    @Test
    @DisplayName("notEmpty(Collection)：非空通过，null/空集合失败")
    void notEmptyCollection() {
        assertThatCode(() -> AssertUtil.notEmpty(List.of(1), "集合不能为空")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.notEmpty(Collections.emptyList(), "集合不能为空"))
                .isInstanceOf(BizException.class).hasMessage("集合不能为空");
        assertThatThrownBy(() -> AssertUtil.notEmpty((List<Integer>) null, "集合不能为空"))
                .isInstanceOf(BizException.class).hasMessage("集合不能为空");
        assertThatThrownBy(() -> AssertUtil.notEmpty(new ArrayList<>(), "集合不能为空"))
                .isInstanceOf(BizException.class).hasMessage("集合不能为空");
    }

    @Test
    @DisplayName("notEmpty(T[])：非空通过，null 与长度为 0 的数组失败")
    void notEmptyArray() {
        assertThatCode(() -> AssertUtil.notEmpty(new String[]{"a"}, "数组不能为空")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.notEmpty(new String[0], "数组不能为空"))
                .isInstanceOf(BizException.class).hasMessage("数组不能为空");
        assertThatThrownBy(() -> AssertUtil.notEmpty((String[]) null, "数组不能为空"))
                .isInstanceOf(BizException.class).hasMessage("数组不能为空");
    }

    @Test
    @DisplayName("inside：元素在集合内通过，不在集合内抛自定义文案")
    void inside() {
        List<String> roles = List.of("ADMIN", "USER");

        assertThatCode(() -> AssertUtil.inside("ADMIN", roles, "角色不合法")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.inside("GUEST", roles, "角色不合法"))
                .isInstanceOf(BizException.class).hasMessage("角色不合法");
    }

    @Test
    @DisplayName("inside：元素或集合为空时先抛出固定的前置校验文案")
    void insidePreconditions() {
        assertThatThrownBy(() -> AssertUtil.inside(null, List.of("A"), "角色不合法"))
                .isInstanceOf(BizException.class).hasMessage("元素不能为空");

        assertThatThrownBy(() -> AssertUtil.inside("A", Collections.emptyList(), "角色不合法"))
                .isInstanceOf(BizException.class).hasMessage("集合不能为空");
    }

    @Test
    @DisplayName("predicate：仅 Boolean.TRUE 通过，false/null 抛自定义文案")
    void predicate() {
        assertThatCode(() -> AssertUtil.predicate(() -> true, "校验失败")).doesNotThrowAnyException();

        assertThatThrownBy(() -> AssertUtil.predicate(() -> false, "校验失败"))
                .isInstanceOf(BizException.class).hasMessage("校验失败");
        assertThatThrownBy(() -> AssertUtil.predicate(() -> null, "校验失败"))
                .isInstanceOf(BizException.class).hasMessage("校验失败");
    }

    @Test
    @DisplayName("predicate：条件为 null 时抛前置校验文案，惰性求值只执行一次")
    void predicatePreconditions() {
        assertThatThrownBy(() -> AssertUtil.predicate(null, "校验失败"))
                .isInstanceOf(BizException.class).hasMessage("断言条件不能为空");

        int[] counter = {0};
        assertThatCode(() -> AssertUtil.predicate(() -> {
            counter[0]++;
            return true;
        }, "校验失败")).doesNotThrowAnyException();
        assertThat(counter).containsExactly(1);
    }
}
