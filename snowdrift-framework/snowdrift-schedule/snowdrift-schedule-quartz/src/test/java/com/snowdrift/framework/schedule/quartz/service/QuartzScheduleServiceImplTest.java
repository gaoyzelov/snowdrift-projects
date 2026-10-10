package com.snowdrift.framework.schedule.quartz.service;

import com.snowdrift.framework.base.util.DateTimeUtil;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.schedule.model.JobDetails;
import com.snowdrift.framework.schedule.quartz.config.QuartzProperties;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobKey;
import com.snowdrift.framework.schedule.quartz.dto.QuartzJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * QuartzScheduleServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Quartz 调度实现黑盒测试：CRUD 全路径、入参校验、重复注册（含并发）、misfire 映射、
 * 更新回滚、状态映射、分页/分组聚合、异常包装与空安全
 * @since 1.0.0
 */
class QuartzScheduleServiceImplTest {

    private Scheduler scheduler;
    private QuartzScheduleServiceImpl service;

    /** 仅用于 addJob/updateJob 的合法 Job 实现 */
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

    private QuartzJobRequest validRequest() {
        QuartzJobRequest request = new QuartzJobRequest();
        request.setName("cleanJob");
        request.setGroup("maintenance");
        request.setCron("0 0 2 * * ?");
        request.setDescription("每日清理");
        request.setJobClass(TestJob.class);
        return request;
    }

    private QuartzJobKey key(String name, String group) {
        return QuartzJobKey.newInstance(name, group);
    }

    // ========== addJob ==========

    @Test
    @DisplayName("addJob：jobClass 为空抛 ScheduleException")
    void addJobRejectsNullJobClass() {
        QuartzJobRequest request = validRequest();
        request.setJobClass(null);

        assertThatThrownBy(() -> service.addJob(request))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务类型不能为空");
    }

    @Test
    @DisplayName("addJob：name/cron 空白抛 ScheduleException")
    void addJobRejectsBlankNameOrCron() {
        QuartzJobRequest noName = validRequest();
        noName.setName("  ");
        assertThatThrownBy(() -> service.addJob(noName))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务名称与 cron 表达式不能为空");

        QuartzJobRequest noCron = validRequest();
        noCron.setCron(null);
        assertThatThrownBy(() -> service.addJob(noCron))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务名称与 cron 表达式不能为空");
    }

    @Test
    @DisplayName("addJob：任务已存在（checkExists=true）抛“任务已存在”")
    void addJobRejectsExisting() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        assertThatThrownBy(() -> service.addJob(validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务已存在");
    }

    @Test
    @DisplayName("addJob：成功注册并返回 key，JobDetail/CronTrigger 承载正确元数据")
    void addJobSuccess() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        QuartzJobRequest request = validRequest();
        request.setParams(Map.of("k", "v"));

        QuartzJobKey result = service.addJob(request);

        assertThat(result.getName()).isEqualTo("cleanJob");
        assertThat(result.getGroup()).isEqualTo("maintenance");

        ArgumentCaptor<JobDetail> detailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(detailCaptor.capture(), triggerCaptor.capture());

        JobDetail detail = detailCaptor.getValue();
        assertThat(detail.getJobClass()).isEqualTo(TestJob.class);
        assertThat(detail.getKey().getName()).isEqualTo("cleanJob");
        assertThat(detail.getKey().getGroup()).isEqualTo("maintenance");
        assertThat(detail.getDescription()).isEqualTo("每日清理");
        assertThat(detail.getJobDataMap().getString("k")).isEqualTo("v");

        assertThat(triggerCaptor.getValue()).isInstanceOf(CronTrigger.class);
        CronTrigger trigger = (CronTrigger) triggerCaptor.getValue();
        assertThat(trigger.getCronExpression()).isEqualTo("0 0 2 * * ?");
    }

    @Test
    @DisplayName("addJob：params 为 null 时 JobDataMap 为空，不抛 NPE")
    void addJobNullParamsYieldsEmptyJobDataMap() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        QuartzJobRequest request = validRequest();
        request.setParams(null);

        service.addJob(request);

        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).scheduleJob(captor.capture(), any(Trigger.class));
        assertThat(captor.getValue().getJobDataMap()).isEmpty();
    }

    @Test
    @DisplayName("addJob：非法 cron 抛 ScheduleException（包装 Quartz RuntimeException）")
    void addJobRejectsInvalidCron() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        QuartzJobRequest request = validRequest();
        request.setCron("not-a-cron");

        assertThatThrownBy(() -> service.addJob(request))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("cron 表达式非法");
    }

    @Test
    @DisplayName("addJob：并发重复注册（ObjectAlreadyExistsException）报“任务已存在”而非笼统失败")
    void addJobConcurrentDuplicate() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenThrow(new ObjectAlreadyExistsException("dup"));

        assertThatThrownBy(() -> service.addJob(validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务已存在");
    }

    @Test
    @DisplayName("addJob：其它 SchedulerException 报“注册失败”")
    void addJobSchedulerException() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.addJob(validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务注册失败");
    }

    @Test
    @DisplayName("addJob：misfire DO_NOTHING 映射为对应触发器指令，默认/null 映射为 FIRE_ONCE_NOW")
    void addJobMisfireMapping() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        QuartzJobRequest doNothing = validRequest();
        doNothing.setMisfireStrategy(MisfireStrategyEnum.DO_NOTHING);
        service.addJob(doNothing);

        QuartzJobRequest fireOnce = validRequest();
        fireOnce.setMisfireStrategy(null);
        service.addJob(fireOnce);

        ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(2)).scheduleJob(any(JobDetail.class), captor.capture());
        List<Trigger> triggers = captor.getAllValues();
        assertThat(((CronTrigger) triggers.get(0)).getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        assertThat(((CronTrigger) triggers.get(1)).getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }

    // ========== removeJob ==========

    @Test
    @DisplayName("removeJob：删除成功")
    void removeJobSuccess() throws SchedulerException {
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        service.removeJob(key("j", "g"));

        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    @DisplayName("removeJob：任务不存在（deleteJob=false）抛异常，避免静默 no-op")
    void removeJobNotExists() throws SchedulerException {
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(false);

        assertThatThrownBy(() -> service.removeJob(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务不存在，无法删除");
    }

    @Test
    @DisplayName("removeJob：SchedulerException 报“删除失败”")
    void removeJobSchedulerException() throws SchedulerException {
        when(scheduler.deleteJob(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.removeJob(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务删除失败");
    }

    // ========== updateJob ==========

    @Test
    @DisplayName("updateJob：jobClass 为空抛“任务类型不能为空”")
    void updateJobRejectsNullJobClass() {
        QuartzJobRequest request = validRequest();
        request.setJobClass(null);

        assertThatThrownBy(() -> service.updateJob(key("cleanJob", "maintenance"), request))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务类型不能为空");
    }

    @Test
    @DisplayName("updateJob：任务不存在抛“任务不存在，无法更新”")
    void updateJobNotExists() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        assertThatThrownBy(() -> service.updateJob(key("cleanJob", "maintenance"), validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务不存在，无法更新");
    }

    @Test
    @DisplayName("updateJob：触发器缺失时显式失败（避免 rescheduleJob 静默 no-op）")
    void updateJobTriggerMissing() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);

        assertThatThrownBy(() -> service.updateJob(key("cleanJob", "maintenance"), validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务触发器不存在，无法更新");
    }

    @Test
    @DisplayName("updateJob：成功先 reschedule 再 addJob")
    void updateJobSuccess() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        CronTrigger original = mock(CronTrigger.class);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(original);

        service.updateJob(key("cleanJob", "maintenance"), validRequest());

        verify(scheduler).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
        verify(scheduler).addJob(any(JobDetail.class), eq(true));
    }

    @Test
    @DisplayName("updateJob：JobDetail 更新失败时回滚 Trigger 并抛“更新失败”")
    void updateJobRollsBackTriggerOnDetailFailure() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        CronTrigger original = mock(CronTrigger.class);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(original);
        doThrow(new SchedulerException("detail boom")).when(scheduler).addJob(any(JobDetail.class), anyBoolean());

        assertThatThrownBy(() -> service.updateJob(key("cleanJob", "maintenance"), validRequest()))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务更新失败");

        // 第 1 次写入新 Trigger，第 2 次回滚为原始 Trigger
        verify(scheduler, times(2)).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
        verify(scheduler).rescheduleJob(any(TriggerKey.class), eq(original));
    }

    // ========== pause / resume / trigger ==========

    @Test
    @DisplayName("pauseJob：存在则暂停；不存在抛异常")
    void pauseJob() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        service.pauseJob(key("j", "g"));
        verify(scheduler).pauseJob(any(JobKey.class));

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        assertThatThrownBy(() -> service.pauseJob(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务不存在，无法暂停");
    }

    @Test
    @DisplayName("resumeJob：存在则恢复；不存在抛异常")
    void resumeJob() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        service.resumeJob(key("j", "g"));
        verify(scheduler).resumeJob(any(JobKey.class));

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        assertThatThrownBy(() -> service.resumeJob(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务不存在，无法恢复");
    }

    @Test
    @DisplayName("triggerJob：透传 params 到 JobDataMap；null 时用空 map")
    void triggerJob() throws SchedulerException {
        service.triggerJob(key("j", "g"), Map.of("p", 1));

        ArgumentCaptor<JobDataMap> captor = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(any(JobKey.class), captor.capture());
        assertThat(captor.getValue().get("p")).isEqualTo(1);

        service.triggerJob(key("j2", "g"), null);
        ArgumentCaptor<JobDataMap> captor2 = ArgumentCaptor.forClass(JobDataMap.class);
        verify(scheduler).triggerJob(eq(JobKey.jobKey("j2", "g")), captor2.capture());
        assertThat(captor2.getValue()).isEmpty();
    }

    @Test
    @DisplayName("triggerJob：SchedulerException 报“触发失败”")
    void triggerJobSchedulerException() throws SchedulerException {
        doThrow(new SchedulerException("boom")).when(scheduler).triggerJob(any(JobKey.class), any(JobDataMap.class));

        assertThatThrownBy(() -> service.triggerJob(key("j", "g"), null))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务触发失败");
    }

    // ========== exists ==========

    @Test
    @DisplayName("exists：透传 checkExists 结果")
    void exists() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        assertThat(service.exists(key("j", "g"))).isTrue();

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        assertThat(service.exists(key("j", "g"))).isFalse();
    }

    @Test
    @DisplayName("exists：SchedulerException 报“查询失败”")
    void existsSchedulerException() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.exists(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务查询失败");
    }

    // ========== getJob ==========

    @Test
    @DisplayName("getJob：detail 不存在返回 null")
    void getJobReturnsNullWhenMissing() throws SchedulerException {
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);

        assertThat(service.getJob(key("j", "g"))).isNull();
        verify(scheduler, never()).getTriggerState(any());
    }

    @Test
    @DisplayName("getJob：CronTrigger 解析 cron、映射状态、空安全填充触发时间")
    void getJobMapsCronTrigger() throws SchedulerException {
        JobDetail detail = mock(JobDetail.class);
        when(detail.getDescription()).thenReturn("desc");
        when(detail.getJobDataMap()).thenReturn(new JobDataMap(Map.of("k", "v")));
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);

        Date prev = new Date(1_600_000_000_000L);
        CronTrigger trigger = mock(CronTrigger.class);
        when(trigger.getCronExpression()).thenReturn("0 0 2 * * ?");
        when(trigger.getPreviousFireTime()).thenReturn(prev);
        when(trigger.getNextFireTime()).thenReturn(null);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(trigger);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NORMAL);

        JobDetails info = service.getJob(key("j", "g"));

        assertThat(info.getName()).isEqualTo("j");
        assertThat(info.getGroup()).isEqualTo("g");
        assertThat(info.getCron()).isEqualTo("0 0 2 * * ?");
        assertThat(info.getDescription()).isEqualTo("desc");
        assertThat(info.getParams()).containsEntry("k", "v");
        assertThat(info.getStatus()).isEqualTo(JobStatusEnum.NORMAL);
        assertThat(info.getLastFireTime()).isEqualTo(DateTimeUtil.dateToLocalDateTime(prev));
        assertThat(info.getNextFireTime()).isNull();
    }

    @Test
    @DisplayName("getJob：非 CronTrigger 时 cron 置空（list/get/exists 口径一致）")
    void getJobNonCronTriggerCronNull() throws SchedulerException {
        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(null);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);

        Trigger simpleTrigger = mock(Trigger.class);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(simpleTrigger);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.PAUSED);

        JobDetails info = service.getJob(key("j", "g"));

        assertThat(info.getCron()).isNull();
        assertThat(info.getParams()).isNull();
        assertThat(info.getStatus()).isEqualTo(JobStatusEnum.PAUSED);
    }

    @Test
    @DisplayName("getJob：trigger 为 null 时触发时间保持 null，仍映射状态")
    void getJobTriggerNull() throws SchedulerException {
        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(new JobDataMap());
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NONE);

        JobDetails info = service.getJob(key("j", "g"));

        assertThat(info.getLastFireTime()).isNull();
        assertThat(info.getNextFireTime()).isNull();
        assertThat(info.getStatus()).isEqualTo(JobStatusEnum.NONE);
    }

    @Test
    @DisplayName("getJob：params 返回副本，外部篡改不影响 JobDetail 持有的 JobDataMap")
    void getJobReturnsParamsCopy() throws SchedulerException {
        JobDataMap original = new JobDataMap(Map.of("k", "v"));
        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(original);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NORMAL);

        JobDetails info = service.getJob(key("j", "g"));
        info.getParams().put("k", "tampered");

        assertThat(original.getString("k")).isEqualTo("v");
        assertThat(info.getParams()).isNotSameAs(original);
    }

    @Test
    @DisplayName("getJob：TriggerState 全量映射到 JobStatusEnum")
    void getJobStatusMapping() throws SchedulerException {
        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(new JobDataMap());
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);

        Map<Trigger.TriggerState, JobStatusEnum> expected = new EnumMap<>(Trigger.TriggerState.class);
        expected.put(Trigger.TriggerState.NORMAL, JobStatusEnum.NORMAL);
        expected.put(Trigger.TriggerState.PAUSED, JobStatusEnum.PAUSED);
        expected.put(Trigger.TriggerState.COMPLETE, JobStatusEnum.COMPLETE);
        expected.put(Trigger.TriggerState.ERROR, JobStatusEnum.ERROR);
        expected.put(Trigger.TriggerState.BLOCKED, JobStatusEnum.BLOCKED);
        expected.put(Trigger.TriggerState.NONE, JobStatusEnum.NONE);

        for (Map.Entry<Trigger.TriggerState, JobStatusEnum> e : expected.entrySet()) {
            when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(e.getKey());
            assertThat(service.getJob(key("j", "g")).getStatus()).isEqualTo(e.getValue());
        }
    }

    @Test
    @DisplayName("getJob：SchedulerException 报“详情查询失败”")
    void getJobSchedulerException() throws SchedulerException {
        when(scheduler.getJobDetail(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.getJob(key("j", "g")))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务详情查询失败");
    }

    // ========== listJobs ==========

    @Test
    @DisplayName("listJobs(group)：聚合分组内任务，跳过 detail 缺失的项")
    void listJobsByGroup() throws SchedulerException {
        JobKey present = JobKey.jobKey("j1", "g");
        JobKey missing = JobKey.jobKey("j2", "g");
        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(new LinkedHashSet<>(List.of(present, missing)));

        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(new JobDataMap());
        when(scheduler.getJobDetail(eq(present))).thenReturn(detail);
        when(scheduler.getJobDetail(eq(missing))).thenReturn(null);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NORMAL);

        List<JobDetails> result = service.listJobs("g");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("j1");
    }

    @Test
    @DisplayName("listJobs()：跨分组聚合")
    void listJobsAll() throws SchedulerException {
        when(scheduler.getJobGroupNames()).thenReturn(List.of("g1"));
        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(Set.of(JobKey.jobKey("j1", "g1")));
        JobDetail detail = mock(JobDetail.class);
        when(detail.getJobDataMap()).thenReturn(new JobDataMap());
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(detail);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NORMAL);

        assertThat(service.listJobs()).hasSize(1);
    }

    @Test
    @DisplayName("listJobs()：getJobGroupNames 抛 SchedulerException 报“列表查询失败”")
    void listJobsSchedulerException() throws SchedulerException {
        when(scheduler.getJobGroupNames()).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.listJobs())
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 任务列表查询失败");
    }

    @Test
    @DisplayName("listJobs(group)：getJobKeys 抛 SchedulerException 报“分组任务列表查询失败”")
    void listJobsByGroupSchedulerException() throws SchedulerException {
        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> service.listJobs("g"))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("Quartz 分组任务列表查询失败");
    }
}
