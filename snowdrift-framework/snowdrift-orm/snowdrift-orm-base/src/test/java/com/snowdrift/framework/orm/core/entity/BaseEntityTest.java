package com.snowdrift.framework.orm.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEntityTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 实体基类黑盒测试：字段默认值、读写、@Data 相等语义，以及 @TableId/@TableLogic
 * 注解契约（主键自增、逻辑删除值）
 * @since 1.0.0
 */
class BaseEntityTest {

    @Test
    @DisplayName("默认值：全部字段为 null（填充与主键均由 MyBatis-Plus 负责）")
    void defaultsAreNull() {
        BaseEntity entity = new BaseEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreateBy()).isNull();
        assertThat(entity.getCreateTime()).isNull();
        assertThat(entity.getUpdateBy()).isNull();
        assertThat(entity.getUpdateTime()).isNull();
        assertThat(entity.getDeleted()).isNull();
    }

    @Test
    @DisplayName("setter/getter：全部字段可读写，时间字段保留纳秒精度")
    void settersAndGetters() {
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 12, 8, 30, 15, 123456789);
        LocalDateTime updateTime = createTime.plusHours(1);

        BaseEntity entity = new BaseEntity();
        entity.setId(100L);
        entity.setCreateBy("alice");
        entity.setCreateTime(createTime);
        entity.setUpdateBy("bob");
        entity.setUpdateTime(updateTime);
        entity.setDeleted(0L);

        assertThat(entity.getId()).isEqualTo(100L);
        assertThat(entity.getCreateBy()).isEqualTo("alice");
        assertThat(entity.getCreateTime()).isEqualTo(createTime);
        assertThat(entity.getUpdateBy()).isEqualTo("bob");
        assertThat(entity.getUpdateTime()).isEqualTo(updateTime);
        assertThat(entity.getDeleted()).isZero();
    }

    @Test
    @DisplayName("@Data 契约：同值相等且 hashCode 一致，主键不同则不相等")
    void equalsAndHashCode() {
        BaseEntity a = new BaseEntity();
        a.setId(1L);
        a.setCreateBy("alice");
        BaseEntity same = new BaseEntity();
        same.setId(1L);
        same.setCreateBy("alice");
        BaseEntity diffId = new BaseEntity();
        diffId.setId(2L);
        diffId.setCreateBy("alice");

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(a).isNotEqualTo(diffId);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a BaseEntity");
    }

    @Test
    @DisplayName("注解契约：id 为主键，IdType.AUTO（数据库自增，不依赖雪花/序列）")
    void tableIdAnnotationIsAuto() throws Exception {
        TableId tableId = field("id").getAnnotation(TableId.class);

        assertThat(tableId).isNotNull();
        assertThat(tableId.type()).isEqualTo(IdType.AUTO);
        assertThat(tableId.value()).isEmpty();
    }

    @Test
    @DisplayName("逻辑删除契约：未删除值为 0，删除值使用 UNIX_TIMESTAMP()")
    void tableLogicAnnotation() throws Exception {
        TableLogic tableLogic = field("deleted").getAnnotation(TableLogic.class);

        assertThat(tableLogic).isNotNull();
        assertThat(tableLogic.value()).isEqualTo("0");
        assertThat(tableLogic.delval()).isEqualTo("UNIX_TIMESTAMP()");
    }

    @Test
    @DisplayName("Serializable 契约：Java 序列化往返后全部字段保持一致")
    void serializationRoundTrip() {
        BaseEntity original = new BaseEntity();
        original.setId(9L);
        original.setCreateBy("alice");
        original.setCreateTime(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        original.setDeleted(0L);

        BaseEntity restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original).isEqualTo(original);
        assertThat(restored.getCreateTime()).isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
    }

    private static Field field(String name) throws NoSuchFieldException {
        return BaseEntity.class.getDeclaredField(name);
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
