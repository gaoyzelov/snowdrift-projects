package com.snowdrift.framework.schedule.model;

import com.snowdrift.framework.schedule.core.IJobKey;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobDetailsTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 任务详情 VO 黑盒测试：默认全 null、配置元数据 + 运行时状态字段读写、@Data 相等语义
 * （含 jobKey/status/时间字段参与比较）、params 无防御性拷贝以及可序列化契约
 * @since 1.0.0
 */
class JobDetailsTest {

    @Test
    @DisplayName("默认值：全部字段为 null（运行时信息查询时才填充）")
    void defaultsAllNull() {
        JobDetails details = new JobDetails();

        assertThat(details.getJobKey()).isNull();
        assertThat(details.getName()).isNull();
        assertThat(details.getGroup()).isNull();
        assertThat(details.getCron()).isNull();
        assertThat(details.getDescription()).isNull();
        assertThat(details.getParams()).isNull();
        assertThat(details.getStatus()).isNull();
        assertThat(details.getLastFireTime()).isNull();
        assertThat(details.getNextFireTime()).isNull();
    }

    @Test
    @DisplayName("setter/getter：配置元数据与运行时状态均可读写")
    void settersAndGetters() {
        LocalDateTime last = LocalDateTime.of(2026, 10, 12, 1, 0);
        LocalDateTime next = last.plusMinutes(5);
        TestJobKey key = new TestJobKey("job-a");

        JobDetails details = new JobDetails();
        details.setJobKey(key);
        details.setName("job-a");
        details.setGroup("default");
        details.setCron("0 0/5 * * * ?");
        details.setDescription("每 5 分钟");
        details.setParams(Map.of("k", "v"));
        details.setStatus(JobStatusEnum.NORMAL);
        details.setLastFireTime(last);
        details.setNextFireTime(next);

        assertThat(details.getJobKey()).isEqualTo(key);
        assertThat(details.getName()).isEqualTo("job-a");
        assertThat(details.getGroup()).isEqualTo("default");
        assertThat(details.getCron()).isEqualTo("0 0/5 * * * ?");
        assertThat(details.getDescription()).isEqualTo("每 5 分钟");
        assertThat(details.getParams()).containsEntry("k", "v");
        assertThat(details.getStatus()).isEqualTo(JobStatusEnum.NORMAL);
        assertThat(details.getLastFireTime()).isEqualTo(last);
        assertThat(details.getNextFireTime()).isEqualTo(next);
    }

    @Test
    @DisplayName("@Data 契约：全部字段参与比较，status/jobKey/时间不同即不相等")
    void equalsAndHashCode() {
        LocalDateTime fireTime = LocalDateTime.of(2026, 10, 12, 1, 0);
        JobDetails base = details(new TestJobKey("k"), JobStatusEnum.NORMAL, fireTime);
        JobDetails same = details(new TestJobKey("k"), JobStatusEnum.NORMAL, fireTime);
        JobDetails diffStatus = details(new TestJobKey("k"), JobStatusEnum.PAUSED, fireTime);
        JobDetails diffKey = details(new TestJobKey("other"), JobStatusEnum.NORMAL, fireTime);
        JobDetails diffTime = details(new TestJobKey("k"), JobStatusEnum.NORMAL, fireTime.plusSeconds(1));

        assertThat(base).isEqualTo(base);
        assertThat(base).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(base).isNotEqualTo(diffStatus);
        assertThat(base).isNotEqualTo(diffKey);
        assertThat(base).isNotEqualTo(diffTime);
        assertThat(base).isNotEqualTo(null);
        assertThat(base).isNotEqualTo("not a JobDetails");
    }

    @Test
    @DisplayName("params：无防御性拷贝，持有外部 Map 引用（外部修改可见）")
    void paramsReferenceRetained() {
        Map<String, Object> params = new HashMap<>();
        params.put("k", "v");

        JobDetails details = new JobDetails();
        details.setParams(params);

        assertThat(details.getParams()).isSameAs(params);

        params.put("k2", "v2");
        assertThat(details.getParams()).containsEntry("k2", "v2");
    }

    @Test
    @DisplayName("Serializable 契约：Java 序列化往返后配置与运行时字段保持一致")
    void serializationRoundTrip() {
        JobDetails original = details(new TestJobKey("k"), JobStatusEnum.ERROR, LocalDateTime.of(2026, 10, 12, 1, 0));
        original.setParams(new HashMap<>(Map.of("k", "v")));

        JobDetails restored = roundTrip(original);

        assertThat(restored).isNotSameAs(original).isEqualTo(original);
        assertThat(restored.getStatus()).isEqualTo(JobStatusEnum.ERROR);
        assertThat(restored.getJobKey().getValue()).isEqualTo("k");
    }

    private static JobDetails details(IJobKey key, JobStatusEnum status, LocalDateTime fireTime) {
        JobDetails details = new JobDetails();
        details.setJobKey(key);
        details.setName("job-a");
        details.setGroup("default");
        details.setStatus(status);
        details.setLastFireTime(fireTime);
        return details;
    }

    /** 测试用任务键：值可为任意对象，并按值比较，便于验证 JobDetails 的相等语义 */
    static class TestJobKey implements IJobKey, Serializable {

        private final Object value;

        TestJobKey(Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestJobKey other)) {
                return false;
            }
            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
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
