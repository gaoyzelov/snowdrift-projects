package com.snowdrift.framework.orm.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantEntityTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 多租户实体基类黑盒测试：继承 BaseEntity、tenantId 默认值与字段声明，
 * 以及 @EqualsAndHashCode(callSuper=true) 下同时比较父类字段与租户字段的相等语义
 * @since 1.0.0
 */
class TenantEntityTest {

    @Test
    @DisplayName("默认值：tenantId 为 null，且继承 BaseEntity 的全部字段（默认亦为 null）")
    void defaultsAndInheritance() {
        TenantEntity entity = new TenantEntity();

        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreateBy()).isNull();
        assertThat(entity.getDeleted()).isNull();
        assertThat(entity).isInstanceOf(BaseEntity.class);
        assertThat(TenantEntity.class.getSuperclass()).isEqualTo(BaseEntity.class);
    }

    @Test
    @DisplayName("字段契约：tenantId 仅声明在 TenantEntity 上，类型为 Long（BaseEntity 无租户字段）")
    void tenantIdFieldDeclaredOnlyOnTenantEntity() throws Exception {
        Field tenantId = TenantEntity.class.getDeclaredField("tenantId");

        assertThat(tenantId.getType()).isEqualTo(Long.class);
        assertThat(TenantEntity.class.getDeclaredFields()).extracting(Field::getName).containsExactly("tenantId");
    }

    @Test
    @DisplayName("相等语义：父类字段与 tenantId 全部参与比较（同 id 不同租户不相等）")
    void equalsIncludesSuperFieldsAndTenantId() {
        TenantEntity a = new TenantEntity();
        a.setId(1L);
        a.setCreateBy("alice");
        a.setTenantId(100L);

        TenantEntity same = new TenantEntity();
        same.setId(1L);
        same.setCreateBy("alice");
        same.setTenantId(100L);

        TenantEntity diffTenant = new TenantEntity();
        diffTenant.setId(1L);
        diffTenant.setCreateBy("alice");
        diffTenant.setTenantId(200L);

        TenantEntity diffBaseField = new TenantEntity();
        diffBaseField.setId(1L);
        diffBaseField.setCreateBy("bob");
        diffBaseField.setTenantId(100L);

        assertThat(a).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(a).isNotEqualTo(diffTenant);
        assertThat(a).isNotEqualTo(diffBaseField);
    }

    @Test
    @DisplayName("跨类比较：基类字段相同的 TenantEntity 与 BaseEntity 仍不相等（canEqual 生效）")
    void notEqualAcrossClasses() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(1L);
        tenant.setTenantId(100L);

        BaseEntity base = new BaseEntity();
        base.setId(1L);

        assertThat(tenant).isNotEqualTo(base);
        assertThat(base).isNotEqualTo(tenant);
    }

    @Test
    @DisplayName("Serializable 契约：Java 序列化往返后父类字段与 tenantId 均保持一致")
    void serializationRoundTrip() {
        TenantEntity original = new TenantEntity();
        original.setId(5L);
        original.setCreateBy("alice");
        original.setTenantId(99L);

        TenantEntity restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original).isEqualTo(original);
        assertThat(restored.getTenantId()).isEqualTo(99L);
        assertThat(restored.getCreateBy()).isEqualTo("alice");
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
