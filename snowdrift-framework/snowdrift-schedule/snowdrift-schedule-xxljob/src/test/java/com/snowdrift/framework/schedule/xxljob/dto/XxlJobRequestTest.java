package com.snowdrift.framework.schedule.xxljob.dto;

import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.enums.BlockStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.enums.RouteStrategyEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XxlJobRequestTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XXL-JOB 任务请求默认值黑盒测试：路由/阻塞策略、超时、重试、triggerStatus 与继承的 misfire 默认
 * @since 1.0.0
 */
class XxlJobRequestTest {

    @Test
    @DisplayName("默认值：RANDOM 路由、SERIAL_EXECUTION 阻塞、timeout=0、retryCount=0、triggerStatus=null、misfire=FIRE_ONCE_NOW")
    void defaults() {
        XxlJobRequest req = new XxlJobRequest();

        assertThat(req.getRouteStrategy()).isEqualTo(RouteStrategyEnum.RANDOM);
        assertThat(req.getBlockStrategy()).isEqualTo(BlockStrategyEnum.SERIAL_EXECUTION);
        assertThat(req.getTimeout()).isZero();
        assertThat(req.getRetryCount()).isZero();
        assertThat(req.getTriggerStatus()).isNull();
        assertThat(req.getMisfireStrategy()).isEqualTo(MisfireStrategyEnum.FIRE_ONCE_NOW);
        assertThat(req.getAuthor()).isNull();
        assertThat(req.getAlarmEmail()).isNull();
    }

    @Test
    @DisplayName("setter 覆盖 XXL-JOB 独有字段")
    void settersOverride() {
        XxlJobRequest req = new XxlJobRequest();
        req.setAuthor("gaoyzelov");
        req.setAlarmEmail("a@b.com");
        req.setRouteStrategy(RouteStrategyEnum.FAILOVER);
        req.setBlockStrategy(BlockStrategyEnum.COVER_EARLY);
        req.setTimeout(30);
        req.setRetryCount(3);
        req.setTriggerStatus(0);

        assertThat(req.getAuthor()).isEqualTo("gaoyzelov");
        assertThat(req.getAlarmEmail()).isEqualTo("a@b.com");
        assertThat(req.getRouteStrategy()).isEqualTo(RouteStrategyEnum.FAILOVER);
        assertThat(req.getBlockStrategy()).isEqualTo(BlockStrategyEnum.COVER_EARLY);
        assertThat(req.getTimeout()).isEqualTo(30);
        assertThat(req.getRetryCount()).isEqualTo(3);
        assertThat(req.getTriggerStatus()).isZero();
    }
}
