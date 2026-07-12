package com.snowdrift.framework.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.snowdrift.framework.common.exception.BizException;
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
 *
 * @author gaoyzelov
 * @date 2026/3/30-13:29
 * @description http工具类
 * @since 1.0.0
 */
@Slf4j
public final class HttpUtil {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
            .build();

    private HttpUtil() {
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
        AssertUtil.notBlank(url, "请求Url不能为空");
        AssertUtil.notNull(formData, "表单数据不能为空");

        String body = buildFormDataBody(formData);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(builder::header);
        }

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
                .uri(URI.create(url))
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
     *
     * @param url 文件 URL
     * @return 文件字节数组
     */
    public static byte[] downloadFile(String url) {
        AssertUtil.notBlank(url, "请求Url不能为空");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            log.debug("DOWNLOAD {} - Status: {}, Size: {} bytes", url, response.statusCode(), response.body().length);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                throw new BizException("HTTP download failed with status: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("Download failed: {}", url, e);
            throw new BizException("HTTP download failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download interrupted: {}", url, e);
            throw new BizException("HTTP download interrupted: " + e.getMessage());
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
                .uri(URI.create(url))
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
        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(builder::header);
        }
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

            log.debug("{} {} - Status: {}, Response: {}, Cost: {}ms",
                    request.method(), request.uri(), response.statusCode(), response.body(), costTime);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                String body = response.body();
                String truncatedBody = body != null && body.length() > 500
                        ? body.substring(0, 500) + "..." : body;
                log.warn("{} request failed with status {}: {}, body: {}",
                        request.method(), response.statusCode(), request.uri(), truncatedBody);
                throw new BizException("HTTP " + request.method() + " request failed with status: "
                        + response.statusCode() + ", body: " + truncatedBody);
            }
        } catch (IOException e) {
            log.error("{} request failed: {}", request.method(), request.uri(), e);
            throw new BizException("HTTP " + request.method() + " request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} request interrupted: {}", request.method(), request.uri(), e);
            throw new BizException("HTTP " + request.method() + " request interrupted: " + e.getMessage());
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
        if (MapUtils.isEmpty(params)) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        boolean hasQuery = url.contains("?");

        if (!hasQuery) {
            sb.append("?");
        } else {
            if (!url.endsWith("?")) {
                sb.append("&");
            }
        }

        params.forEach((key, value) -> {
            if (sb.charAt(sb.length() - 1) != '?' && sb.charAt(sb.length() - 1) != '&') {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });

        return sb.toString();
    }

    /**
     * 构建 Form 表单请求体
     *
     * @param formData 表单数据
     * @return 编码后的表单字符串
     */
    public static String buildFormDataBody(Map<String, String> formData) {
        if (MapUtils.isEmpty(formData)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        formData.forEach((key, value) -> {
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
     * 检查 URL 是否可访问
     *
     * @param url     请求 URL
     * @param timeout 超时时间
     * @return true-可访问，false-不可访问
     */
    public static boolean isAccessible(String url, Duration timeout) {
        AssertUtil.notBlank(url, "请求Url不能为空");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .build();

            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            log.warn("URL check failed: {}", url, e);
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
     * 创建带代理的 HttpClient（用于特殊网络环境）
     *
     * @param proxyHost 代理主机
     * @param proxyPort 代理端口
     * @return HttpClient 实例
     */
    public static HttpClient createProxyClient(String proxyHost, int proxyPort) {
        AssertUtil.notBlank(proxyHost, "代理主机不能为空");
        AssertUtil.isTrue(proxyPort > 0 && proxyPort <= 65535, "代理端口必须在1-65535之间");

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

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }
}
