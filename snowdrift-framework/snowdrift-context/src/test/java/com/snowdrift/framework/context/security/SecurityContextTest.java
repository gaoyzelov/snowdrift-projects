package com.snowdrift.framework.context.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityContextTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 安全上下文模型单元测试
 * @since 1.0.0
 */
class SecurityContextTest {

    @Test
    @DisplayName("builder 可填充身份、组织、授权与扩展信息")
    void builderFillsAllFields() {
        SecurityContext context = SecurityContext.builder()
                .userId(1L)
                .username("admin")
                .nickname("管理员")
                .tenantId(100L)
                .deptId(10L)
                .roleKeys(List.of("admin", "user"))
                .permissions(List.of("sys:user:list"))
                .platform("web")
                .attributes(Map.of("k", "v"))
                .build();

        assertThat(context.getUserId()).isEqualTo(1L);
        assertThat(context.getUsername()).isEqualTo("admin");
        assertThat(context.getNickname()).isEqualTo("管理员");
        assertThat(context.getTenantId()).isEqualTo(100L);
        assertThat(context.getDeptId()).isEqualTo(10L);
        assertThat(context.getRoleKeys()).containsExactly("admin", "user");
        assertThat(context.getPermissions()).containsExactly("sys:user:list");
        assertThat(context.getPlatform()).isEqualTo("web");
        assertThat(context.getAttributes()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("空上下文：所有字段为 null，可作匿名场景的兜底")
    void emptyContext() {
        SecurityContext context = SecurityContext.builder().build();

        assertThat(context.getUserId()).isNull();
        assertThat(context.getUsername()).isNull();
        assertThat(context.getRoleKeys()).isNull();
        assertThat(context.getAttributes()).isNull();
    }

    @Test
    @DisplayName("toString 排除 attributes，避免令牌等机密随日志外泄")
    void toStringExcludesAttributes() {
        SecurityContext context = SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员")
                .attributes(Map.of("token", "SECRET-JWT"))
                .build();

        String text = context.toString();
        assertThat(text).contains("userId=1", "username=admin", "nickname=管理员");
        assertThat(text).doesNotContain("SECRET-JWT", "attributes", "token");
    }

    @Test
    @DisplayName("equals/hashCode 仍包含被 toString 排除的字段")
    void equalsIncludesAttributes() {
        SecurityContext a = SecurityContext.builder().userId(1L).attributes(Map.of("token", "t1")).build();
        SecurityContext b = SecurityContext.builder().userId(1L).attributes(Map.of("token", "t1")).build();
        SecurityContext different = SecurityContext.builder().userId(1L).attributes(Map.of("token", "t2")).build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(different);
    }

    @Test
    @DisplayName("提供公开无参构造器：Jackson 等反序列化框架可实例化（Redis TokenStore 曾因此读回失败）")
    void hasPublicNoArgConstructor() throws Exception {
        Constructor<SecurityContext> noArg = SecurityContext.class.getDeclaredConstructor();
        assertThat(Modifier.isPublic(noArg.getModifiers())).isTrue();

        // 模拟 Jackson 的行为：无参构造 + setter 填充
        SecurityContext created = noArg.newInstance();
        created.setUserId(1L);
        created.setUsername("admin");
        created.setAttributes(Map.of("token", "t"));

        assertThat(created.getUserId()).isEqualTo(1L);
        assertThat(created.getUsername()).isEqualTo("admin");
        assertThat(created.getAttributes()).containsEntry("token", "t");

        // @Builder 在全参构造器存在的前提下仍可用
        assertThat(SecurityContext.builder().userId(2L).build().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("copy：字段等价但实例与容器均独立，改副本不影响原对象")
    void copyIsFieldEquivalentButContainerIndependent() {
        SecurityContext original = SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员")
                .tenantId(100L).deptId(10L)
                .roleKeys(new java.util.ArrayList<>(List.of("admin")))
                .permissions(new java.util.ArrayList<>(List.of("sys:user:list")))
                .platform("web")
                .attributes(new HashMap<>(Map.of("token", "t")))
                .build();

        SecurityContext copy = original.copy();

        assertThat(copy).isEqualTo(original).isNotSameAs(original);
        assertThat(copy.getRoleKeys()).isNotSameAs(original.getRoleKeys());
        assertThat(copy.getPermissions()).isNotSameAs(original.getPermissions());
        assertThat(copy.getAttributes()).isNotSameAs(original.getAttributes());

        copy.setTenantId(999L);
        copy.getRoleKeys().add("hacked");
        copy.getAttributes().put("injected", "x");

        assertThat(original.getTenantId()).isEqualTo(100L);
        assertThat(original.getRoleKeys()).containsExactly("admin");
        assertThat(original.getAttributes()).containsOnlyKeys("token");
    }

    @Test
    @DisplayName("copy：空字段的上下文复制后仍为空，不抛异常")
    void copyOfEmptyContext() {
        SecurityContext copy = SecurityContext.builder().build().copy();

        assertThat(copy.getUserId()).isNull();
        assertThat(copy.getRoleKeys()).isNull();
        assertThat(copy.getPermissions()).isNull();
        assertThat(copy.getAttributes()).isNull();
    }

    @Test
    @DisplayName("Serializable：Java 序列化往返后字段一致（Dubbo/缓存传递的前提）")
    void javaSerializationRoundTrip() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("token", "abc");

        SecurityContext context = SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员")
                .tenantId(100L).deptId(10L)
                .roleKeys(List.of("admin")).permissions(List.of("sys:user:list"))
                .platform("web").attributes(attributes)
                .build();

        SecurityContext copy = roundTrip(context);

        assertThat(copy).isEqualTo(context).isNotSameAs(context);
        assertThat(copy.getAttributes()).containsEntry("token", "abc");
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
