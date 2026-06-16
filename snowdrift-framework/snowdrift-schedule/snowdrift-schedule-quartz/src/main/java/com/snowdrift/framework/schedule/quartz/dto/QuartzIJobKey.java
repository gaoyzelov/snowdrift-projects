package com.snowdrift.framework.schedule.quartz.dto;

import com.snowdrift.framework.schedule.core.IJobKey;
import lombok.Data;

/**
 * Quartz 任务标识 — 基于 name + group 定位任务（对应 {@code org.quartz.JobKey}）
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
public class QuartzIJobKey implements IJobKey {

    /** 任务名称 */
    private String name;
    /** 任务分组 */
    private String group;

    private QuartzIJobKey(){}

    public static QuartzIJobKey newInstance(String name, String group) {
        QuartzIJobKey quartzIJobKey = new QuartzIJobKey();
        quartzIJobKey.setName(name);
        quartzIJobKey.setGroup(group);
        return quartzIJobKey;
    }
}
