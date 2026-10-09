package com.snowdrift.framework.context.http;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * HttpContext
 *
 * @author gaoyzelov
 * @date 2026/4/30-15:59
 * @description HTTP 上下文
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求URI
     */
    private String uri;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 请求 UA
     */
    private String userAgent;

    /**
     * 请求 IP
     */
    private String ip;

    /**
     * IP 归属地
     */
    private String ipLocation;

    /**
     * 请求参数
     * <p>排除出 {@code toString()}：这里是 Servlet 参数的原样副本（{@code HttpContextFilter} 直接填入），
     * 登录等接口会带明文口令，上下文对象一旦被打印就会随日志外泄。</p>
     */
    @ToString.Exclude
    private Map<String, String> paramMap;
}
