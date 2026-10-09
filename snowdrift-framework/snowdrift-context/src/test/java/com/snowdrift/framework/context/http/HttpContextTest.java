package com.snowdrift.framework.context.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpContextTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description HTTP 上下文模型单元测试
 * @since 1.0.0
 */
class HttpContextTest {

    @Test
    @DisplayName("builder 可填充全部字段")
    void builderFillsAllFields() {
        Map<String, String> params = Map.of("page", "1");

        HttpContext context = HttpContext.builder()
                .uri("/api/user")
                .method("GET")
                .userAgent("Mozilla/5.0")
                .ip("1.2.3.4")
                .ipLocation("中国 北京")
                .paramMap(params)
                .build();

        assertThat(context.getUri()).isEqualTo("/api/user");
        assertThat(context.getMethod()).isEqualTo("GET");
        assertThat(context.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(context.getIp()).isEqualTo("1.2.3.4");
        assertThat(context.getIpLocation()).isEqualTo("中国 北京");
        assertThat(context.getParamMap()).isEqualTo(params);
    }

    @Test
    @DisplayName("builder 未设置的字段保持 null")
    void unsetFieldsAreNull() {
        HttpContext context = HttpContext.builder().build();

        assertThat(context.getUri()).isNull();
        assertThat(context.getMethod()).isNull();
        assertThat(context.getParamMap()).isNull();
    }

    @Test
    @DisplayName("setter 可修改字段（模型可变，供跨线程共享）")
    void settersAreAvailable() {
        HttpContext context = HttpContext.builder().build();
        context.setIp("10.0.0.1");
        context.setParamMap(Map.of("k", "v"));

        assertThat(context.getIp()).isEqualTo("10.0.0.1");
        assertThat(context.getParamMap()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("equals/hashCode 依据全部字段")
    void equalsAndHashCode() {
        HttpContext a = HttpContext.builder().uri("/a").ip("1.2.3.4").build();
        HttpContext b = HttpContext.builder().uri("/a").ip("1.2.3.4").build();
        HttpContext different = HttpContext.builder().uri("/b").ip("1.2.3.4").build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(different);
    }

    @Test
    @DisplayName("toString 排除 paramMap，避免明文口令等随日志外泄")
    void toStringExcludesParamMap() {
        HttpContext context = HttpContext.builder()
                .uri("/login").method("POST").ip("1.2.3.4")
                .paramMap(Map.of("username", "admin", "password", "P@ssw0rd!"))
                .build();

        String text = context.toString();
        assertThat(text).contains("uri=/login", "method=POST", "ip=1.2.3.4");
        assertThat(text).doesNotContain("P@ssw0rd!", "password", "paramMap");
    }

    @Test
    @DisplayName("paramMap 仅被排除出 toString，equals/hashCode 仍参与比较")
    void equalsStillIncludesParamMap() {
        HttpContext a = HttpContext.builder().uri("/a").paramMap(Map.of("k", "v1")).build();
        HttpContext b = HttpContext.builder().uri("/a").paramMap(Map.of("k", "v1")).build();
        HttpContext different = HttpContext.builder().uri("/a").paramMap(Map.of("k", "v2")).build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(different);
    }

    @Test
    @DisplayName("提供公开无参构造器：Jackson 等反序列化框架可实例化")
    void hasPublicNoArgConstructor() throws Exception {
        Constructor<HttpContext> noArg = HttpContext.class.getDeclaredConstructor();
        assertThat(Modifier.isPublic(noArg.getModifiers())).isTrue();

        HttpContext created = noArg.newInstance();
        created.setUri("/a");
        created.setParamMap(Map.of("k", "v"));

        assertThat(created.getUri()).isEqualTo("/a");
        assertThat(created.getParamMap()).containsEntry("k", "v");
        assertThat(HttpContext.builder().uri("/b").build().getUri()).isEqualTo("/b");
    }

    @Test
    @DisplayName("Serializable：Java 序列化往返后字段一致（跨进程/缓存传递的前提）")
    void javaSerializationRoundTrip() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("page", "1");

        HttpContext context = HttpContext.builder()
                .uri("/api/user").method("GET").userAgent("ua").ip("1.2.3.4")
                .ipLocation("中国 北京").paramMap(params)
                .build();

        HttpContext copy = roundTrip(context);

        assertThat(copy).isEqualTo(context).isNotSameAs(context);
        assertThat(copy.getParamMap()).isEqualTo(params);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
