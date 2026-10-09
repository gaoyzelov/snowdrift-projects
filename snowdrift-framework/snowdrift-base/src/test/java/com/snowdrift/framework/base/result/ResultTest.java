package com.snowdrift.framework.base.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 统一返回结果单元测试
 * @since 1.0.0
 */
class ResultTest {

    @Test
    @DisplayName("ok() 使用 OK 码与内置消息，data 为 null")
    void okWithoutData() {
        long before = System.currentTimeMillis();
        Result<Void> result = Result.ok();
        long after = System.currentTimeMillis();

        assertThat(result.getCode()).isEqualTo(ResultCode.OK.code());
        assertThat(result.getMsg()).isEqualTo(ResultCode.OK.msg());
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isBetween(before, after);
    }

    @Test
    @DisplayName("ok(data) 携带数据并保留 OK 码")
    void okWithData() {
        Result<String> result = Result.ok("payload");

        assertThat(result.getCode()).isEqualTo(ResultCode.OK.code());
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    @DisplayName("ok(msg, data) 使用自定义成功消息")
    void okWithCustomMessage() {
        Result<String> result = Result.ok("已更新", "payload");

        assertThat(result.getCode()).isEqualTo(ResultCode.OK.code());
        assertThat(result.getMsg()).isEqualTo("已更新");
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    @DisplayName("err() 系列使用 ERR 码，且不回填 data")
    void errVariants() {
        assertThat(Result.err().getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(Result.err().getMsg()).isEqualTo(ResultCode.ERR.msg());
        assertThat(Result.err().getData()).isNull();

        Result<Void> withMsg = Result.err("订单不存在");
        assertThat(withMsg.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(withMsg.getMsg()).isEqualTo("订单不存在");
        assertThat(withMsg.getData()).isNull();

        Result<String> withCode = Result.err(40001, "订单不存在");
        assertThat(withCode.getCode()).isEqualTo(40001);
        assertThat(withCode.getMsg()).isEqualTo("订单不存在");

        Result<String> msgAndData = Result.err("部分失败", "detail");
        assertThat(msgAndData.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(msgAndData.getData()).isEqualTo("detail");
    }

    @Test
    @DisplayName("err(ResultCode) 使用结果码自带的码与消息")
    void errWithResultCode() {
        Result<Void> result = Result.err(ResultCode.NOT_FOUND);
        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.code());
        assertThat(result.getMsg()).isEqualTo(ResultCode.NOT_FOUND.msg());

        Result<String> withData = Result.err(ResultCode.CONFLICT, "冲突明细");
        assertThat(withData.getCode()).isEqualTo(ResultCode.CONFLICT.code());
        assertThat(withData.getData()).isEqualTo("冲突明细");
    }

    @Test
    @DisplayName("create 支持完全自定义，timestamp 由框架填写")
    void createCustomizesAllFields() {
        Result<Integer> result = Result.create(5000, "自定义", 42);

        assertThat(result.getCode()).isEqualTo(5000);
        assertThat(result.getMsg()).isEqualTo("自定义");
        assertThat(result.getData()).isEqualTo(42);
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("链式 setter 可继续修改工厂产出的对象")
    void chainedSetters() {
        Result<String> result = Result.ok("data").setMsg("追加提示").setCode(1000).setTimestamp(1L);

        assertThat(result.getMsg()).isEqualTo("追加提示");
        assertThat(result.getCode()).isEqualTo(1000);
        assertThat(result.getTimestamp()).isEqualTo(1L);
    }

    @Test
    @DisplayName("equals/hashCode 依据全部字段（Lombok @Data 语义）")
    void equalsAndHashCode() {
        Result<String> a = Result.create(1, "ok", "x");
        Result<String> b = Result.create(1, "ok", "x").setTimestamp(a.getTimestamp());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(Result.create(2, "ok", "x").setTimestamp(a.getTimestamp()));
    }
}
