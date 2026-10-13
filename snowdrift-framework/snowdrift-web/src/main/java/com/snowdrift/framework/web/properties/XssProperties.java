package com.snowdrift.framework.web.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.List;

/**
 * XSS 防护配置属性
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "snowdrift.xss")
public class XssProperties implements Serializable {

    /**
     * 是否启用 XSS 防护（默认关闭）
     * <p><b>开启后的实际覆盖面：</b>过滤器 {@code XssFilter} 清洗请求参数与 Header；
     * {@code XssJsonBodyAdvice} 仅清洗 {@code @RequestBody} 反序列化后为 Map/集合/数组的 String 值，
     * <b>不覆盖 DTO 等复杂 POJO 的字段</b>。默认注入的清洗器做 HTML 实体转义，其自身文档即说明不适合
     * JSON 输出（会改变载荷内容、可能双重编码）。XSS 属输出上下文问题，建议在渲染/响应阶段按上下文编码。</p>
     */
    @NotNull
    private Boolean enabled = Boolean.FALSE;

    /**
     * 是否对<b>请求参数</b>做清洗（默认关闭）
     * <p>
     * 输入侧清洗会<b>改写业务数据</b>：合法包含 {@code & < > " '} 的参数到达 Controller 时已被 HTML
     * 实体转义（{@code a&b} → {@code a&amp;b}），既污染审计与日志里记录的入参，也可能破坏签名校验、
     * 令牌解析等对原值敏感的链路。而 XSS 属<b>输出上下文</b>问题，应在渲染/响应阶段按上下文编码，
     * 本模块的 {@code XssJsonBodyAdvice} 与默认清洗器的 Javadoc 也是这个立场。
     * </p>
     * <p>
     * 保留该开关是为了让依赖旧行为的下游平滑迁移；新接入方建议保持关闭。
     * </p>
     */
    @NotNull
    private Boolean escapeParameters = Boolean.FALSE;

    /**
     * 是否对<b>请求 Header</b> 做清洗（默认关闭，且独立于 {@link #escapeParameters}）
     * <p>
     * 输入侧转义会破坏 Header 的协议语义值：{@code If-None-Match: "v1"} 变成 {@code &quot;v1&quot;}
     * （条件请求与缓存失效）、{@code Authorization: Bearer a.b&c} 变成 {@code Bearer a.b&amp;c}
     * （令牌被改写，认证失败）。Header 名始终不转义 —— 它是查值的键。
     * </p>
     * <p>
     * 与 {@link #escapeParameters} 一样，保留该开关是为了让依赖旧行为的下游平滑迁移；
     * 新接入方建议保持关闭，改在输出侧按上下文编码。
     * </p>
     */
    @NotNull
    private Boolean escapeHeaders = Boolean.FALSE;

    /**
     * 排除路径（Ant 风格，如 /admin/richtext/**），不进行 XSS 过滤
     */
    private List<String> excludePathPatterns = List.of();
}
