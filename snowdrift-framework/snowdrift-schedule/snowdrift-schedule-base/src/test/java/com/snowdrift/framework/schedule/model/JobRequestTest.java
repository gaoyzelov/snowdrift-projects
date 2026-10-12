package com.snowdrift.framework.schedule.model;

import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobRequestTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 公共任务请求 DTO 黑盒测试：misfireStrategy 默认 FIRE_ONCE_NOW（README 契约）、字段可读写，
 * 覆盖 @Data 相等语义、misfireStrategy 可置空、params 无防御性拷贝与空白字符串边界
 * @since 1.0.0
 */
class JobRequestTest {

    @Test
    @DisplayName("默认 misfireStrategy 为 FIRE_ONCE_NOW，其余字段默认 null")
    void defaults() {
        JobRequest request = new JobRequest();

        assertThat(request.getMisfireStrategy()).isEqualTo(MisfireStrategyEnum.FIRE_ONCE_NOW);
        assertThat(request.getName()).isNull();
        assertThat(request.getGroup()).isNull();
        assertThat(request.getCron()).isNull();
        assertThat(request.getDescription()).isNull();
        assertThat(request.getParams()).isNull();
    }

    @Test
    @DisplayName("setter 覆盖默认值")
    void settersOverrideDefaults() {
        JobRequest request = new JobRequest();
        request.setName("job");
        request.setGroup("grp");
        request.setCron("0 0 2 * * ?");
        request.setDescription("desc");
        request.setParams(Map.of("k", "v"));
        request.setMisfireStrategy(MisfireStrategyEnum.DO_NOTHING);

        assertThat(request.getName()).isEqualTo("job");
        assertThat(request.getGroup()).isEqualTo("grp");
        assertThat(request.getCron()).isEqualTo("0 0 2 * * ?");
        assertThat(request.getDescription()).isEqualTo("desc");
        assertThat(request.getParams()).containsEntry("k", "v");
        assertThat(request.getMisfireStrategy()).isEqualTo(MisfireStrategyEnum.DO_NOTHING);
    }

    @Test
    @DisplayName("@Data 契约：全部字段参与比较，cron 不同即不相等")
    void equalsAndHashCode() {
        JobRequest base = request("job", "grp", "0 0 2 * * ?");
        JobRequest same = request("job", "grp", "0 0 2 * * ?");
        JobRequest diffCron = request("job", "grp", "0 0 3 * * ?");

        assertThat(base).isEqualTo(base);
        assertThat(base).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(base).isNotEqualTo(diffCron);
        assertThat(base).isNotEqualTo(null);
        assertThat(base).isNotEqualTo("not a JobRequest");
    }

    @Test
    @DisplayName("边界：misfireStrategy 可显式置 null；params 无防御性拷贝，持有外部 Map 引用")
    void nullableStrategyAndParamsReference() {
        JobRequest request = new JobRequest();
        request.setMisfireStrategy(null);
        assertThat(request.getMisfireStrategy()).isNull();

        Map<String, Object> params = new HashMap<>();
        params.put("k", "v");
        request.setParams(params);

        assertThat(request.getParams()).isSameAs(params);
        params.put("k2", "v2");
        assertThat(request.getParams()).containsEntry("k2", "v2");
    }

    @Test
    @DisplayName("边界：name/group/cron 为空串或空白串时不抛异常（DTO 层不做校验）")
    void blankStringsAccepted() {
        JobRequest request = new JobRequest();
        request.setName("");
        request.setGroup("   ");
        request.setCron("");

        assertThat(request.getName()).isEmpty();
        assertThat(request.getGroup()).isEqualTo("   ");
        assertThat(request.getCron()).isEmpty();
    }

    private static JobRequest request(String name, String group, String cron) {
        JobRequest request = new JobRequest();
        request.setName(name);
        request.setGroup(group);
        request.setCron(cron);
        return request;
    }
}
