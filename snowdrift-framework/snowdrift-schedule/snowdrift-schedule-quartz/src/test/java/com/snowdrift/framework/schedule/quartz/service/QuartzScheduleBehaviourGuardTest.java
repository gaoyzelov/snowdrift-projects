package com.snowdrift.framework.schedule.quartz.service;

import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.schedule.quartz.config.QuartzProperties;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobKey;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QuartzScheduleBehaviourGuardTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description Quartz 调度语义守卫：手动触发必须做存在性检查、更新不得静默忽略改名、
 *              JobDetail 不得被意外置为 durable
 * @since 1.0.0
 */
class QuartzScheduleBehaviourGuardTest {

    private Scheduler scheduler;
    private QuartzScheduleServiceImpl service;

    /** 仅用于 updateJob 的合法 Job 实现 */
    public static class TestJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            // no-op
        }
    }

    @BeforeEach
    void setUp() {
        scheduler = mock(Scheduler.class);
        service = new QuartzScheduleServiceImpl(scheduler, new QuartzProperties());
    }

    private QuartzJobRequest request(String name, String group) {
        QuartzJobRequest request = new QuartzJobRequest();
        request.setName(name);
        request.setGroup(group);
        request.setCron("0 0 2 * * ?");
        request.setDescription("每日清理");
        request.setJobClass(TestJob.class);
        return request;
    }

    private QuartzJobKey key(String name, String group) {
        return QuartzJobKey.newInstance(name, group);
    }

    @Test
    @DisplayName("triggerJob：任务不存在时必须报错 —— 其它方法都有检查，唯独手动触发没有")
    void triggerJobRejectsMissingJob() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        assertThatThrownBy(() -> service.triggerJob(key("cleanJob", "maintenance"), Map.of()))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("updateJob：请求名与 jobKey 不一致（改名）时必须显式失败，不能静默沿用旧名")
    void updateJobRejectsRename() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(mock(Trigger.class));

        assertThatThrownBy(() -> service.updateJob(key("cleanJob", "maintenance"), request("renamed", "maintenance")))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("改名");
    }

    @Test
    @DisplayName("updateJob：JobDetail 不得被置为 durable，否则改变任务的持久化语义")
    void updateJobKeepsJobNonDurable() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(mock(Trigger.class));

        service.updateJob(key("cleanJob", "maintenance"), request("cleanJob", "maintenance"));

        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).addJob(captor.capture(), eq(true));
        assertThat(captor.getValue().isDurable()).isFalse();
        assertThat(captor.getValue().getKey()).isEqualTo(JobKey.jobKey("cleanJob", "maintenance"));
    }
}
