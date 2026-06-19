package com.snowdrift.framework.schedule.core;

import com.snowdrift.framework.schedule.dto.JobRequest;
import com.snowdrift.framework.schedule.dto.JobDetails;

import java.util.List;
import java.util.Map;

/**
 * 统一调度操作接口
 * <p>
 * 屏蔽 Quartz / XXL-JOB 差异，提供运行时的动态任务管理能力。
 * 所有任务通过管理端调用此接口进行 CRUD，不依赖注解扫描。
 * </p>
 *
 * @param <T> 任务请求 DTO 类型（Quartz: {@code QuartzJobRequest}，XXL-JOB: {@code XxlJobRequest}）
 * @param <K> 任务标识类型（Quartz: {@code QuartzIJobKey}，XXL-JOB: {@code XxlIJobKey}）
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
public interface IScheduleService<T extends JobRequest, K extends IJobKey> {

    // ========== 任务管理 ==========

    K addJob(T request);

    void removeJob(K jobKey);

    void updateJob(K jobKey, T request);

    void pauseJob(K jobKey);

    void resumeJob(K jobKey);

    void triggerJob(K jobKey, Map<String, Object> params);

    // ========== 查询 ==========

    boolean exists(K jobKey);

    JobDetails getJob(K jobKey);

    List<JobDetails> listJobs();

    List<JobDetails> listJobs(String group);
}
