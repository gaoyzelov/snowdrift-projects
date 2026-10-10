package com.snowdrift.framework.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 重复提交防护注解
 * <p>
 * 在方法执行前写入防重标记，若在指定时间窗口内再次提交相同请求则拒绝，
 * 用于实现接口幂等性。最终缓存 key 为：
 * {@code repeat:submit:ip:<客户端IP>:<请求URI>:<作用域>}。
 * </p>
 * <p>
 * <b>作用域（{@link #key()}）的两种来源：</b>
 * <ul>
 *   <li>未配置（默认）：取「业务参数指纹」（业务参数经字段序列化后取 MD5，请求绑定类参数与
 *       文件按「文件名 + 大小」处理），适用于绝大多数"同一份表单/JSON 重复提交"的场景；</li>
 *   <li>配置为 SpEL：取表达式结果，用于显式收窄/放大防重范围（如只按订单号防重）。</li>
 * </ul>
 * 两种来源外层都带「客户端 IP + 请求 URI」，因此不同用户、不同接口不会互相误伤。
 * </p>
 * <p><b>放行（不防重）的情况：</b>非 Web 环境；客户端 IP 无法识别；作用域生成失败
 * （SpEL 解析失败、参数序列化失败）。此时切面记日志后直接执行业务。</p>
 * <p><b>后端要求：</b>防重依赖 per-key 过期能力，Redis 与 Caffeine 后端都支持
 * （Caffeine 通过 {@code VarExpiration} 实现）。注意 Caffeine 是进程内缓存，
 * 多实例部署时防重只在单个 JVM 内生效，跨实例防重请使用 Redis。</p>
 * <p><b>{@link #interval()} 必须大于 0</b>：为 0 时标记会立即过期、防重窗口为空，
 * 所有重复请求都会被放行，因此该配置会在切面中直接抛 {@code BizException}。</p>
 * <p><b>安全边界：</b>身份取自可被伪造的 {@code X-Forwarded-For}（见 {@code IpUtil#getIp}），
 * 防重只防"手抖/连点"，不防恶意绕过；需要强幂等请在业务侧加数据库唯一键，或让
 * {@link #key()} 带上用户维度。</p>
 *
 * <pre>
 * // 默认：按「参数指纹」防重（推荐，无需配置）
 * &#064;RepeatSubmit(interval = 5)
 * public Result&lt;Void&gt; createOrder(OrderCreateDTO dto) { ... }
 *
 * // 按业务字段收窄防重范围
 * &#064;RepeatSubmit(key = "#orderNo", interval = 5)
 * public Result&lt;Void&gt; createOrder(String orderNo) { ... }
 *
 * // 基于用户 + 业务防重
 * &#064;RepeatSubmit(key = "'user:submit:' + #userId", interval = 60)
 * public Result&lt;Void&gt; submit(Long userId) { ... }
 * </pre>
 *
 * @author gaoyzelov
 * @date 2026/6/2
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 防重 key 的「作用域」，支持 SpEL 表达式；留空则使用业务参数指纹
     * <p>该值只是最终 key 的最后一段，前面还会拼上客户端 IP 与请求 URI，
     * 因此无需自己写用户/接口维度；也不需要写成"全局唯一"的形态。</p>
     */
    String key() default "";

    /**
     * 防重时间间隔（秒），在间隔内重复提交会被拒绝
     */
    long interval() default 5;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复提交时的提示信息
     */
    String message() default "请勿重复提交，请稍后重试";
}
