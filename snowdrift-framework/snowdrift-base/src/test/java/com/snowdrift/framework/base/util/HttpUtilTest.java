package com.snowdrift.framework.base.util;

import com.alibaba.fastjson2.TypeReference;
import com.snowdrift.framework.base.exception.BizException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HttpUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description HTTP 工具类单元测试（使用 JDK 内置 HttpServer，不依赖外部网络）
 * @since 1.0.0
 */
class HttpUtilTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/get", exchange -> respond(exchange, 200, "hello"));
        server.createContext("/json", exchange -> respond(exchange, 200, "{\"id\":7,\"name\":\"order\"}"));
        server.createContext("/jsonList", exchange -> respond(exchange, 200, "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]"));
        server.createContext("/echo", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String query = exchange.getRequestURI().getRawQuery();
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String custom = exchange.getRequestHeaders().getFirst("X-Custom");
            respond(exchange, 200, "method=" + exchange.getRequestMethod()
                    + ";query=" + (query == null ? "" : query)
                    + ";contentType=" + (contentType == null ? "" : contentType)
                    + ";custom=" + (custom == null ? "" : custom)
                    + ";body=" + body);
        });
        server.createContext("/bytes", exchange -> {
            byte[] payload = {1, 2, 3, 4, 5};
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.createContext("/fail", exchange -> respond(exchange, 500, "boom"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "not found"));
        server.createContext("/slow", exchange -> {
            sleep(1_500);
            respond(exchange, 200, "slow");
        });

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 用于 JSON 反序列化的简单载体 */
    public static class Order {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Nested
    @DisplayName("请求与响应")
    class Requests {

        @Test
        void get() {
            assertThat(HttpUtil.get(baseUrl + "/get")).isEqualTo("hello");
        }

        @Test
        @DisplayName("GET 追加查询参数，已存在的查询串保持原样")
        void getWithParams() {
            // 用 LinkedHashMap 固定遍历顺序，Map.of 不保证顺序
            Map<String, String> params = new LinkedHashMap<>();
            params.put("a", "1");
            params.put("b", "x y");

            String response = HttpUtil.get(baseUrl + "/echo", params, Map.of("X-Custom", "v"), 10);

            assertThat(response).contains("method=GET", "query=a=1&b=x+y", "custom=v");
        }

        @Test
        @DisplayName("已有查询串时以 & 追加，且不重新编码原参数")
        void getAppendsToExistingQuery() {
            String response = HttpUtil.get(baseUrl + "/echo?keep=raw%20value", Map.of("a", "1"), null, 10);

            assertThat(response).contains("query=keep=raw%20value&a=1");
        }

        @Test
        void post() {
            assertThat(HttpUtil.post(baseUrl + "/echo", "payload")).contains("method=POST", "body=payload");
        }

        @Test
        @DisplayName("POST 带自定义请求头")
        void postWithHeaders() {
            assertThat(HttpUtil.post(baseUrl + "/echo", "x", Map.of("X-Custom", "abc"))).contains("custom=abc");
        }

        @Test
        void postJson() {
            Order order = new Order();
            order.setId(7);
            order.setName("order");

            String response = HttpUtil.postJson(baseUrl + "/echo", order);
            assertThat(response).contains("method=POST", "contentType=application/json", "\"id\":7");
        }

        @Test
        void postForm() {
            Map<String, String> form = new HashMap<>();
            form.put("username", "admin");
            form.put("password", "p w");

            String response = HttpUtil.postForm(baseUrl + "/echo", form);
            assertThat(response).contains("method=POST", "contentType=application/x-www-form-urlencoded",
                    "username=admin", "password=p+w");
        }

        @Test
        void putAndDelete() {
            assertThat(HttpUtil.put(baseUrl + "/echo", "p")).contains("method=PUT", "body=p");
            assertThat(HttpUtil.delete(baseUrl + "/echo")).contains("method=DELETE", "body=");
        }

        @Test
        void getForObject() {
            Order order = HttpUtil.getForObject(baseUrl + "/json", Order.class);

            assertThat(order.getId()).isEqualTo(7);
            assertThat(order.getName()).isEqualTo("order");
        }

        @Test
        @DisplayName("泛型反序列化使用 TypeReference")
        void getForObjectWithTypeReference() {
            List<Order> orders = HttpUtil.getForObject(baseUrl + "/jsonList", new TypeReference<List<Order>>() {
            });

            assertThat(orders).hasSize(2);
            assertThat(orders).extracting(Order::getName).containsExactly("a", "b");
        }

        @Test
        void postFormForObject() {
            Order order = HttpUtil.postFormForObject(baseUrl + "/json", Map.of("k", "v"), Order.class);
            assertThat(order.getId()).isEqualTo(7);
        }

        @Test
        void postJsonForObject() {
            Order order = HttpUtil.postJsonForObject(baseUrl + "/json", Map.of("k", "v"), Order.class);
            assertThat(order.getName()).isEqualTo("order");
        }

        @Test
        void downloadFile() {
            assertThat(HttpUtil.downloadFile(baseUrl + "/bytes")).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("非 2xx 抛 BizException 并带上状态码")
        void nonSuccessStatus() {
            assertThatThrownBy(() -> HttpUtil.get(baseUrl + "/missing"))
                    .isInstanceOf(BizException.class).hasMessage("HTTP请求失败，status: 404");
            assertThatThrownBy(() -> HttpUtil.get(baseUrl + "/fail"))
                    .isInstanceOf(BizException.class).hasMessage("HTTP请求失败，status: 500");
            assertThatThrownBy(() -> HttpUtil.downloadFile(baseUrl + "/fail"))
                    .isInstanceOf(BizException.class).hasMessage("HTTP下载失败，status: 500");
        }

        @Test
        @DisplayName("连接失败（端口未监听）抛 BizException 且不泄漏底层异常")
        void connectionFailure() {
            assertThatThrownBy(() -> HttpUtil.get("http://127.0.0.1:1/"))
                    .isInstanceOf(BizException.class).hasMessage("HTTP请求失败");
        }

        @Test
        @DisplayName("请求超时抛 BizException")
        void timeout() {
            assertThatThrownBy(() -> HttpUtil.get(baseUrl + "/slow", null, null, 1))
                    .isInstanceOf(BizException.class).hasMessage("HTTP请求失败");
        }

        @Test
        @DisplayName("线程被中断时恢复中断标记并抛 BizException")
        void interrupted() throws Exception {
            AtomicReference<Throwable> thrown = new AtomicReference<>();
            AtomicBoolean interruptFlagRestored = new AtomicBoolean();

            Thread worker = new Thread(() -> {
                try {
                    HttpUtil.get(baseUrl + "/slow");
                } catch (Throwable e) {
                    thrown.set(e);
                    interruptFlagRestored.set(Thread.currentThread().isInterrupted());
                }
            });
            worker.start();
            Thread.sleep(300);
            worker.interrupt();
            worker.join(10_000);

            assertThat(thrown.get()).isInstanceOf(BizException.class).hasMessage("HTTP请求中断");
            assertThat(interruptFlagRestored).isTrue();
        }
    }

    @Nested
    @DisplayName("isAccessible")
    class Accessible {

        @Test
        @DisplayName("2xx/3xx 为 true")
        void reachable() {
            assertThat(HttpUtil.isAccessible(baseUrl + "/get")).isTrue();
            assertThat(HttpUtil.isAccessible(baseUrl + "/get", Duration.ofSeconds(5))).isTrue();
        }

        @Test
        @DisplayName("4xx/5xx、地址非法与连接失败均为 false，不抛异常")
        void unreachable() {
            assertThat(HttpUtil.isAccessible(baseUrl + "/missing")).isFalse();
            assertThat(HttpUtil.isAccessible(baseUrl + "/fail")).isFalse();
            assertThat(HttpUtil.isAccessible("not-a-url")).isFalse();
            assertThat(HttpUtil.isAccessible("http://127.0.0.1:1/")).isFalse();
        }

        @Test
        @DisplayName("超时与空值校验")
        void timeoutAndValidation() {
            assertThat(HttpUtil.isAccessible(baseUrl + "/slow", Duration.ofMillis(200))).isFalse();

            assertThatThrownBy(() -> HttpUtil.isAccessible(baseUrl + "/get", Duration.ZERO))
                    .isInstanceOf(BizException.class).hasMessage("超时时间必须大于 0");
            assertThatThrownBy(() -> HttpUtil.isAccessible(baseUrl + "/get", null))
                    .isInstanceOf(BizException.class).hasMessage("超时时间不能为空");
        }
    }

    @Nested
    @DisplayName("地址与参数校验")
    class Validation {

        @Test
        @DisplayName("仅接受 http/https，且要求主机非空")
        void rejectsUnsupportedUrls() {
            assertThatThrownBy(() -> HttpUtil.get("example.com/path"))
                    .isInstanceOf(BizException.class).hasMessage("仅支持 http/https 请求地址");
            assertThatThrownBy(() -> HttpUtil.get("ftp://example.com"))
                    .isInstanceOf(BizException.class).hasMessage("仅支持 http/https 请求地址");
            assertThatThrownBy(() -> HttpUtil.get("http:///path"))
                    .isInstanceOf(BizException.class).hasMessage("请求地址格式错误");
            assertThatThrownBy(() -> HttpUtil.get("not a url"))
                    .isInstanceOf(BizException.class).hasMessage("请求地址格式错误");
            assertThatThrownBy(() -> HttpUtil.get("  "))
                    .isInstanceOf(BizException.class).hasMessage("请求Url不能为空");
        }

        @Test
        @DisplayName("协议大小写不敏感")
        void schemeIsCaseInsensitive() {
            assertThat(HttpUtil.get(baseUrl.replaceFirst("http", "HTTP") + "/get")).isEqualTo("hello");
        }

        @Test
        @DisplayName("JDK 受限请求头转为 BizException")
        void rejectsRestrictedHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Host", "example.com");

            assertThatThrownBy(() -> HttpUtil.post(baseUrl + "/echo", "{}", headers))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("请求头不合法");
        }

        @Test
        @DisplayName("超时参数必须大于 0")
        void rejectsNonPositiveTimeout() {
            assertThatThrownBy(() -> HttpUtil.get(baseUrl + "/get", Map.of(), Map.of(), 0))
                    .isInstanceOf(BizException.class).hasMessage("超时时间必须大于 0");
            assertThatThrownBy(() -> HttpUtil.get(baseUrl + "/get", Map.of(), Map.of(), -1))
                    .isInstanceOf(BizException.class).hasMessage("超时时间必须大于 0");
        }

        @Test
        void buildUrlWithParams() {
            assertThat(HttpUtil.buildUrlWithParams("http://h/p", null)).isEqualTo("http://h/p");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p", Map.of())).isEqualTo("http://h/p");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p", Map.of("a", "1"))).isEqualTo("http://h/p?a=1");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p?x=1", Map.of("a", "1"))).isEqualTo("http://h/p?x=1&a=1");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p?", Map.of("a", "1"))).isEqualTo("http://h/p?a=1");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p?x=1&", Map.of("a", "1"))).isEqualTo("http://h/p?x=1&a=1");
            assertThat(HttpUtil.buildUrlWithParams("http://h/p#frag", Map.of("a", "1"))).isEqualTo("http://h/p?a=1#frag");
        }

        @Test
        @DisplayName("表单体按 UTF-8 编码，参数名/值不能为 null")
        void buildFormDataBody() {
            assertThat(HttpUtil.buildFormDataBody(Map.of())).isEmpty();
            assertThat(HttpUtil.buildFormDataBody(null)).isEmpty();
            assertThat(HttpUtil.buildFormDataBody(Map.of("a b", "c&d"))).isEqualTo("a+b=c%26d");

            Map<String, String> nullValue = new HashMap<>();
            nullValue.put("a", null);
            assertThatThrownBy(() -> HttpUtil.buildFormDataBody(nullValue))
                    .isInstanceOf(BizException.class).hasMessage("参数值不能为空");
        }

        @Test
        @DisplayName("Basic Auth 头为 Base64(user:password)")
        void basicAuth() {
            assertThat(HttpUtil.getBasicAuthHeader("user", "pwd"))
                    .isEqualTo("Basic " + java.util.Base64.getEncoder()
                            .encodeToString("user:pwd".getBytes(StandardCharsets.UTF_8)));

            assertThatThrownBy(() -> HttpUtil.getBasicAuthHeader("us:er", "pwd"))
                    .isInstanceOf(BizException.class).hasMessage("Basic Auth 用户名不能包含冒号");
            assertThatThrownBy(() -> HttpUtil.getBasicAuthHeader(" ", "pwd"))
                    .isInstanceOf(BizException.class).hasMessage("用户名不能为空");
            assertThatThrownBy(() -> HttpUtil.getBasicAuthHeader("user", ""))
                    .isInstanceOf(BizException.class).hasMessage("密码不能为空");
        }

        @Test
        @DisplayName("代理客户端参数校验")
        void proxyClientValidation() {
            assertThat(HttpUtil.createProxyClient("127.0.0.1", 8080)).isNotNull();

            assertThatThrownBy(() -> HttpUtil.createProxyClient(" ", 8080))
                    .isInstanceOf(BizException.class).hasMessage("代理主机不能为空");
            assertThatThrownBy(() -> HttpUtil.createProxyClient("127.0.0.1", 0))
                    .isInstanceOf(BizException.class).hasMessage("代理端口必须在1-65535之间");
            assertThatThrownBy(() -> HttpUtil.createProxyClient("127.0.0.1", 65536))
                    .isInstanceOf(BizException.class).hasMessage("代理端口必须在1-65535之间");
        }
    }
}
