package com.snowdrift.framework.schedule.model;

import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JobRequestTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 公共任务请求 DTO 黑盒测试：misfireStrategy 默认 FIRE_ONCE_NOW（README 契约），字段可读写
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
}
