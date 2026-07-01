package com.snowdrift.framework.schedule.xxljob.dto;

import com.snowdrift.framework.schedule.core.IJobKey;
import lombok.Data;

/**
 * XXL-JOB 任务标识 — 基于 id + groupId 定位任务（对应 Admin 端 jobId + jobGroup）
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
public class XxlJobKey implements IJobKey {

    /** 任务 ID（XXL-JOB Admin 端唯一标识） */
    private Integer id;

    /** 执行器分组 ID（XXL-JOB Admin 端 jobGroup） */
    private Integer groupId;

    private XxlJobKey(){}

    public static XxlJobKey newInstance(int id, int groupId) {
        XxlJobKey xxlJobKey = new XxlJobKey();
        xxlJobKey.setId(id);
        xxlJobKey.setGroupId(groupId);
        return xxlJobKey;
    }

    @Override
    public Object getValue() {
        return id;
    }
}
