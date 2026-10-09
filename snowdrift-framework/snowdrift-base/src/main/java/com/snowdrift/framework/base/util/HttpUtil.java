package com.snowdrift.framework.base.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * HttpUtil
 * <p>
 * 基于 JDK {@link HttpClient} 的轻量 HTTP 工具类。
 * </p>
 * <p>
 * <b>注意：响应体无大小上限。</b>所有请求方法的响应体都会完整读入内存
 * （{@code BodyHandlers.ofString()} / {@code ofByteArray()}），因此请求 URL 不可信、
 * 或响应体规模不可控时存在内存耗尽风险；此类场景请勿直接使用本类拉取，
 * 或由调用方在外部施加限制（超时、限流、可信 URL 白名单等）。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/3/30-13:29
 * @description http工具类
 * @since 1.0.0
 */
@Slf4j
public final class HttpUtil {

    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_RANGE = 100;

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private HttpUtil() {
    }

    /**
     * 校验并解析请求地址
     * <p>
     * 除语法校验外，还要求协议为 http/https 且主机非空：{@code URI.create} 接受相对地址与非
     * http(s) 协议，这类地址要到 {@code HttpRequest.Builder.uri} 才会暴露，且抛出的是
     * {@code IllegalArgumentException}，会绕过本类的 {@code BizException} 约定。
     * </p>
     *
     * @param url 请求地址
     * @return URI
     * @throws BizException 地址格式非法或协议不受支持时抛出
     */
    private static URI toUri(String url) {
        AssertUtil.notBlank(url, "请求Url不能为空");
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!SCHEME_HTTP.equalsIgnoreCase(scheme) && !SCHEME_HTTPS.equalsIgnoreCase(scheme)) {
                log.error("请求地址协议不受支持：scheme={}", scheme);
                return BizException.throwEx("仅支持 http/https 请求地址");
            }
            if (uri.getHost() == null) {
                return BizException.throwEx("请求地址格式错误");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            log.error("请求地址格式错误：url={}, err={}", url, e.getMessage());
            return BizException.throwEx("请求地址格式错误");
        }
    }

    /**
     * 获取 HttpClient
     *
     * @return HttpClient
     */
    public static HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    /**
     * GET 请求
     *
     * @param url 请求 URL
     * @return 响应内容
     */
    public static String get(String url) {
        return request(url, GET, null, null, DEFAULT_TIMEOUT);
    }


    /**
     * GET 请求
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> headers) {
        return request(url, GET, null, headers, DEFAULT_TIMEOUT);
    }

    /**
     * GET 请求
     *
     * @param url     请求 URL
     * @param params  查询参数
     * @param headers 请求头
     * @param timeout 超时时间（秒）
     * @return 响应内容
     */
    public static String get(String url, Map<String, String> params, Map<String, String> headers, long timeout) {
        AssertUtil.predicate(() -> timeout > 0, "超时时间必须大于 0");
        String fullUrl = buildUrlWithParams(url, params);
        return request(fullUrl, GET, null, headers, Duration.ofSeconds(timeout));
    }


    /**
     * POST 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return 响应内容
     */
    public static String post(String url, String body) {
        return request(url, POST, body, null, DEFAULT_TIMEOUT);
    }

    /**
     * POST 请求
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public static String post(String url, String body, Map<String, String> headers) {
        return request(url, POST, body, headers, DEFAULT_TIMEOUT);
    }

    /**
     * POST Form 表单请求
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> formData) {
        return postForm(url, formData, null);
    }

    /**
     * POST Form 表单请求
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @param headers  请求头
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers) {
        return postForm(url, formData, headers, DEFAULT_TIMEOUT);
    }

    /**
     * POST Form 表单请求（指定超时）
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @param headers  请求头
     * @param timeout  请求超时
     * @return 响应内容
     */
    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers, Duration timeout) {
        validateTimeout(timeout);
        AssertUtil.notBlank(url, "请求Url不能为空");
        AssertUtil.notNull(formData, "表单数据不能为空");
        String body = buildFormDataBody(formData);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(toUri(url))
                .timeout(timeout)
                .header("Content-Type", CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        setHeaders(builder, headers);

        return sendRequest(builder.build());
    }

    /**
     * POST JSON 请求
     *
     * @param url  请求 URL
     * @param data 数据对象
     * @return 响应内容
     */
    public static String postJson(String url, Object data) {
        AssertUtil.notBlank(url, "请求Url不能为空");
        AssertUtil.notNull(data, "请求体不能为空");

        String json = JSON.toJSONString(data);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(toUri(url))
                .timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        return sendRequest(builder.build());
    }

    /**
     * PUT 请求
     *
     * @param url  请求 URL
     * @param body 请求体
     * @return 响应内容
     */
    public static String put(String url, String body) {
        return request(url, PUT, body, null, DEFAULT_TIMEOUT);
    }

    /**
     * PUT 请求
     *
     * @param url     请求 URL
     * @param body    请求体
     * @param headers 请求头
     * @return 响应内容
     */
    public static String put(String url, String body, Map<String, String> headers) {
        return request(url, PUT, body, headers, DEFAULT_TIMEOUT);
    }

    /**
     * DELETE 请求
     *
     * @param url 请求 URL
     * @return 响应内容
     */
    public static String delete(String url) {
        return request(url, DELETE, null, null, DEFAULT_TIMEOUT);
    }

    /**
     * DELETE 请求
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应内容
     */
    public static String delete(String url, Map<String, String> headers) {
        return request(url, DELETE, null, headers, DEFAULT_TIMEOUT);
    }

    /**
     * GET 请求并解析为 JSON 对象
     *
     * @param url   请求 URL
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象实例
     */
    public static <T> T getForObject(String url, Class<T> clazz) {
        String json = get(url);
        return JSON.parseObject(json, clazz);
    }

    /**
     * GET 请求并解析为 JSON 对象
     *
     * @param url     请求 URL
     * @param typeRef 类型引用
     * @param <T>     泛型
     * @return 对象实例
     */
    public static <T> T getForObject(String url, TypeReference<T> typeRef) {
        String json = get(url);
        return JSON.parseObject(json, typeRef);
    }

    /**
     * POST 请求并解析为 JSON 对象
     *
     * @param url   请求 URL
     * @param body  请求体
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象实例
     */
    public static <T> T postForObject(String url, String body, Class<T> clazz) {
        String json = post(url, body);
        return JSON.parseObject(json, clazz);
    }


    /**
     * POST Form 表单请求并解析为 JSON 对象
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @param clazz    目标类型
     * @param <T>      泛型
     * @return 对象实例
     */
    public static <T> T postFormForObject(String url, Map<String, String> formData, Class<T> clazz) {
        String json = postForm(url, formData);
        return JSON.parseObject(json, clazz);
    }


    /**
     * POST JSON 请求并解析为 JSON 对象
     *
     * @param url   请求 URL
     * @param data  数据对象
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象实例
     */
    public static <T> T postJsonForObject(String url, Object data, Class<T> clazz) {
        String json = postJson(url, data);
        return JSON.parseObject(json, clazz);
    }

    /**
     * 下载文件到字节数组
     * <p>
     * <b>无响应体大小上限</b>：响应内容会完整读入内存（{@code BodyHandlers.ofByteArray()}），
     * URL 不可信或文件过大时存在内存耗尽风险。调用方须自行保证 URL 可信；
     * 大文件请改用流式下载或施加外部限制。
     * </p>
     *
     * @param url 文件 URL
     * @return 文件字节数组
     */
    public static byte[] downloadFile(String url) {
        AssertUtil.notBlank(url, "请求Url不能为空");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(toUri(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            log.debug("HTTP下载 - Status: {}, Size: {} bytes", response.statusCode(), response.body().length);

            if (response.statusCode() >= HTTP_SUCCESS_MIN && response.statusCode() < HTTP_SUCCESS_MIN + HTTP_SUCCESS_RANGE) {
                return response.body();
            } else {
                throw new BizException("HTTP下载失败，status: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("HTTP下载失败: {}", url, e);
            throw new BizException("HTTP下载失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP下载中断: {}", url, e);
            throw new BizException("HTTP下载中断");
        }
    }

    /**
     * 通用 HTTP 请求方法
     *
     * @param url     请求 URL
     * @param method  请求方法
     * @param body    请求体
     * @param headers 请求头
     * @param timeout 超时时间
     * @return 响应内容
     */
    private static String request(String url, String method, String body, Map<String, String> headers, Duration timeout) {
        return request(url, method, body, headers, timeout, HTTP_CLIENT);
    }

    /**
     * 通用 HTTP 请求方法
     *
     * @param url     请求 URL
     * @param method  请求方法
     * @param body    请求体
     * @param headers 请求头
     * @param timeout 超时时间
     * @return 响应内容
     */
    private static String request(String url, String method, String body, Map<String, String> headers, Duration timeout, HttpClient client) {
        AssertUtil.notBlank(url, "请求Url不能为空");
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(toUri(url))
                .timeout(timeout);
        if (StringUtils.isNotBlank(body)) {
            if (POST.equalsIgnoreCase(method) || PUT.equalsIgnoreCase(method)) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        setHeaders(builder, headers);
        return sendRequest(builder.build(), client);
    }

    /**
     * 执行 HTTP 请求
     *
     * @param request HTTP 请求
     * @return 响应内容
     */
    private static String sendRequest(HttpRequest request) {
        return sendRequest(request, HTTP_CLIENT);
    }

    /**
     * 执行 HTTP 请求（支持自定义 HttpClient）
     * <p>响应体通过 {@code BodyHandlers.ofString()} 完整读入内存，无大小上限。</p>
     *
     * @param request HTTP 请求
     * @param client  HttpClient 实例
     * @return 响应内容
     */
    private static String sendRequest(HttpRequest request, HttpClient client) {
        long startTime = System.currentTimeMillis();
        HttpClient httpClient = client != null ? client : HTTP_CLIENT;

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long costTime = System.currentTimeMillis() - startTime;

            log.debug("HTTP {} - Status: {}, Cost: {}ms", request.method(), response.statusCode(), costTime);

            if (response.statusCode() >= HTTP_SUCCESS_MIN && response.statusCode() < HTTP_SUCCESS_MIN + HTTP_SUCCESS_RANGE) {
                return response.body();
            } else {
                log.warn("HTTP {} 请求失败，status {}", request.method(), response.statusCode());
                throw new BizException("HTTP请求失败，status: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("HTTP {} 请求失败: url={}, err={}", request.method(), request.uri(), e.getMessage());
            return BizException.throwEx("HTTP请求失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP {} 请求中断: url={}, err={}", request.method(), request.uri(), e.getMessage());
            return BizException.throwEx("HTTP请求中断");
        }
    }

    /**
     * 构建带参数的 URL
     *
     * @param url    原始 URL
     * @param params 参数映射
     * @return 完整的 URL
     */
    public static String buildUrlWithParams(String url, Map<String, String> params) {
        toUri(url);
        if (MapUtils.isEmpty(params)) {
            return url;
        }

        int fragmentIndex = url.indexOf('#');
        String base = fragmentIndex < 0 ? url : url.substring(0, fragmentIndex);
        String fragment = fragmentIndex < 0 ? "" : url.substring(fragmentIndex);
        String separator = !base.contains("?") ? "?"
                : (base.endsWith("?") || base.endsWith("&") ? "" : "&");
        return base + separator + buildFormDataBody(params) + fragment;
    }

    /**
     * 构建 Form 表单请求体
     *
     * @param formData 表单数据
     * @return 编码后的表单字符串
     */
    public static String buildFormDataBody(Map<String, String> formData) {
        if (MapUtils.isEmpty(formData)) {
            return StrConst.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        formData.forEach((key, value) -> {
            AssertUtil.notNull(key, "参数名不能为空");
            AssertUtil.notNull(value, "参数值不能为空");
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    /**
     * 检查 URL 是否可访问（2xx/3xx 返回 true，使用 GET 并丢弃响应体）。
     *
     * @param url     请求 URL
     * @param timeout 超时时间
     * @return true-可访问，false-不可访问
     */
    public static boolean isAccessible(String url, Duration timeout) {
        AssertUtil.notBlank(url, "请求Url不能为空");
        validateTimeout(timeout);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(toUri(url))
                    .timeout(timeout)
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.debug("URL检查失败: err={}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 URL 是否可访问（使用默认超时）
     *
     * @param url 请求 URL
     * @return true-可访问，false-不可访问
     */
    public static boolean isAccessible(String url) {
        return isAccessible(url, Duration.ofSeconds(5));
    }

    /**
     * 设置请求头
     * <p>
     * JDK 的 {@code setHeader} 对 {@code Host}、{@code Content-Length} 等受限头会抛
     * {@code IllegalArgumentException}（直接转发 Servlet 头映射时很容易命中），
     * 这里统一转换为 {@code BizException}，避免绕过本类的异常约定。
     * </p>
     *
     * @param builder 请求构造器
     * @param headers 请求头（可为空）
     */
    private static void setHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (MapUtils.isEmpty(headers)) {
            return;
        }
        try {
            headers.forEach(builder::setHeader);
        } catch (IllegalArgumentException e) {
            log.error("请求头不合法：err={}", e.getMessage());
            BizException.throwEx("请求头不合法：" + e.getMessage());
        }
    }

    /**
     * 校验超时时间
     */
    private static void validateTimeout(Duration timeout) {
        AssertUtil.notNull(timeout, "超时时间不能为空");
        AssertUtil.predicate(() -> !timeout.isZero() && !timeout.isNegative(), "超时时间必须大于 0");
    }

    /**
     * 创建带代理的 HttpClient（用于特殊网络环境）。
     *
     * @param proxyHost 代理主机
     * @param proxyPort 代理端口
     * @return HttpClient 实例，调用方直接使用其 send/sendAsync 方法
     */
    public static HttpClient createProxyClient(String proxyHost, int proxyPort) {
        AssertUtil.notBlank(proxyHost, "代理主机不能为空");
        AssertUtil.predicate(() -> proxyPort > 0 && proxyPort <= 65535, "代理端口必须在1-65535之间");

        InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .proxy(java.net.ProxySelector.of(proxyAddress))
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .build();
    }


    /**
     * 添加 Basic Authentication 请求头
     *
     * @param username 用户名
     * @param password 密码
     * @return Basic Auth 头值
     */
    public static String getBasicAuthHeader(String username, String password) {
        AssertUtil.notBlank(username, "用户名不能为空");
        AssertUtil.notBlank(password, "密码不能为空");
        AssertUtil.predicate(() -> !username.contains(StrConst.COLON), "Basic Auth 用户名不能包含冒号");

        String auth = username + StrConst.COLON + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }
}
