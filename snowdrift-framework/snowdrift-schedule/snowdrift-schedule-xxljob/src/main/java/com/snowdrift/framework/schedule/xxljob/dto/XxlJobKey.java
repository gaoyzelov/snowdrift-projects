package com.snowdrift.framework.schedule.xxljob.dto;

import com.snowdrift.framework.schedule.core.IJobKey;
import lombok.Data;

/**
 * XXL-JOB 任务标识 — 基于数字 id 定位任务（对应 Admin 端 {@code jobId}）
 *
 * @author 83674
 * @date 2026/6/15
 * @since 1.0.0
 */
@Data
public class XxlJobKey implements IJobKey {

    /** 任务 ID（XXL-JOB Admin 端唯一标识） */
    private Integer id;

    private XxlJobKey(){}

    public static XxlJobKey newInstance(int id) {
        XxlJobKey xxlJobKey = new XxlJobKey();
        xxlJobKey.setId(id);
        return xxlJobKey;
    }

    @Override
    public Object getValue() {
        return id;
    }
}
