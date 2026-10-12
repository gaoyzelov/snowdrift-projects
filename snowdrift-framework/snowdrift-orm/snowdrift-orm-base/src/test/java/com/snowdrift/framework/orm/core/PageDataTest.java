package com.snowdrift.framework.orm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageDataTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 分页数据黑盒测试：@Builder 构造、null/空集合/极值边界、@Data 相等语义与可序列化契约
 * @since 1.0.0
 */
class PageDataTest {

    @Test
    @DisplayName("Builder：total 与 records 被如实写入")
    void builderSetsFields() {
        PageData<String> data = PageData.<String>builder()
                .total(42L)
                .records(List.of("a", "b"))
                .build();

        assertThat(data.getTotal()).isEqualTo(42L);
        assertThat(data.getRecords()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Builder：不设值时 total/records 均为 null，由调用方决定兜底")
    void builderDefaultsToNull() {
        PageData<Object> data = PageData.<Object>builder().build();

        assertThat(data.getTotal()).isNull();
        assertThat(data.getRecords()).isNull();
    }

    @Test
    @DisplayName("边界：total=0、records=空列表时保持原样，不把空列表替换为 null")
    void zeroTotalAndEmptyRecords() {
        PageData<String> data = PageData.<String>builder()
                .total(0L)
                .records(List.of())
                .build();

        assertThat(data.getTotal()).isZero();
        assertThat(data.getRecords()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("边界：total=Long.MAX_VALUE 如实保存，且未参与分页换算")
    void extremeTotalPreserved() {
        PageData<String> data = PageData.<String>builder().total(Long.MAX_VALUE).build();

        assertThat(data.getTotal()).isEqualTo(Long.MAX_VALUE);
        assertThat(data.getRecords()).isNull();
    }

    @Test
    @DisplayName("@Data 契约：同值相等且 hashCode 一致，字段不同/自反/null/异类语义正确")
    void equalsAndHashCodeSemantics() {
        PageData<String> a = PageData.<String>builder().total(1L).records(List.of("x")).build();
        PageData<String> b = PageData.<String>builder().total(1L).records(List.of("x")).build();
        PageData<String> diffTotal = PageData.<String>builder().total(2L).records(List.of("x")).build();
        PageData<String> diffRecords = PageData.<String>builder().total(1L).records(List.of("y")).build();

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(diffTotal);
        assertThat(a).isNotEqualTo(diffRecords);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a PageData");
    }

    @Test
    @DisplayName("Serializable 契约：Java 序列化往返后 total/records 保持一致")
    void serializationRoundTrip() {
        PageData<String> original = PageData.<String>builder()
                .total(7L)
                .records(List.of("a", "b"))
                .build();

        PageData<String> restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original).isEqualTo(original);
        assertThat(restored.getTotal()).isEqualTo(7L);
        assertThat(restored.getRecords()).containsExactly("a", "b");
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject(value);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Serializable 往返失败", e);
        }
    }
}
