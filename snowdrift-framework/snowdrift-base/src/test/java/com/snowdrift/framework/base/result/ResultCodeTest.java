package com.snowdrift.framework.base.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultCodeTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 业务状态码单元测试
 * @since 1.0.0
 */
class ResultCodeTest {

    /** 通过反射取出全部 ResultCode 常量，避免逐个硬编码后遗漏 */
    private static List<ResultCode> allCodes() throws IllegalAccessException {
        List<ResultCode> codes = new ArrayList<>();
        for (Field field : ResultCode.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == ResultCode.class) {
                codes.add((ResultCode) field.get(null));
            }
        }
        return codes;
    }

    @Test
    @DisplayName("OK=1、ERR=0，与 HTTP 状态码刻意区分")
    void generalCodes() {
        assertThat(ResultCode.OK.code()).isEqualTo(1);
        assertThat(ResultCode.OK.msg()).isEqualTo("操作成功");
        assertThat(ResultCode.ERR.code()).isZero();
        assertThat(ResultCode.ERR.msg()).isEqualTo("操作失败");
    }

    @Test
    @DisplayName("业务码不落在 HTTP 状态码区间（100~599）")
    void codesAvoidHttpRange() throws IllegalAccessException {
        for (ResultCode code : allCodes()) {
            assertThat(code.code())
                    .as("业务码 %d(%s) 不应落在 HTTP 状态码区间", code.code(), code.msg())
                    .satisfiesAnyOf(
                            value -> assertThat(value).isLessThan(100),
                            value -> assertThat(value).isGreaterThan(599));
        }
    }

    @Test
    @DisplayName("全部结果码互不重复，且消息非空")
    void codesAreUniqueAndDocumented() throws IllegalAccessException {
        List<ResultCode> codes = allCodes();
        assertThat(codes).isNotEmpty();

        assertThat(codes).extracting(ResultCode::code).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(code -> assertThat(code.msg()).isNotBlank());
    }

    @Test
    @DisplayName("record 语义：同码同消息相等，可自定义业务码")
    void recordSemantics() {
        assertThat(new ResultCode(1000, "请求参数有误，请检查后重试")).isEqualTo(ResultCode.BAD_REQUEST);
        assertThat(ResultCode.BAD_REQUEST).isNotEqualTo(ResultCode.UNAUTHORIZED);

        ResultCode custom = new ResultCode(40001, "订单不存在");
        assertThat(custom.code()).isEqualTo(40001);
        assertThat(custom.msg()).isEqualTo("订单不存在");
    }

    @Test
    @DisplayName("常用错误码取值稳定（下游按数值对接）")
    void documentedCodeValues() {
        assertThat(ResultCode.BAD_REQUEST.code()).isEqualTo(1000);
        assertThat(ResultCode.UNAUTHORIZED.code()).isEqualTo(1001);
        assertThat(ResultCode.FORBIDDEN.code()).isEqualTo(1002);
        assertThat(ResultCode.NOT_FOUND.code()).isEqualTo(1003);
        assertThat(ResultCode.METHOD_NOT_ALLOWED.code()).isEqualTo(1004);
        assertThat(ResultCode.CONFLICT.code()).isEqualTo(1005);
        assertThat(ResultCode.LOCK_FAILED.code()).isEqualTo(1006);
        assertThat(ResultCode.PAYLOAD_TOO_LARGE.code()).isEqualTo(1007);
        assertThat(ResultCode.UNSUPPORTED_MEDIA_TYPE.code()).isEqualTo(1008);
        assertThat(ResultCode.TOO_MANY_REQUESTS.code()).isEqualTo(1009);
        assertThat(ResultCode.INTERNAL_SERVER_ERROR.code()).isEqualTo(2000);
        assertThat(ResultCode.SERVICE_UNAVAILABLE.code()).isEqualTo(2001);
    }
}
