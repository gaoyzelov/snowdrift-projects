package com.snowdrift.framework.context.http;

import lombok.Builder;
import lombok.Data;

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
     * IP地址
     */
    private String ipLocation;
    /**
     * 请求参数
     */
    private Map<String, String> paramMap;
}
