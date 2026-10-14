package com.snowdrift.social.dingtalk.event;

import com.alibaba.fastjson2.JSONObject;

/**
 *
 * @author 83674
 * @date 2026/10/13-17:34
 * @description
 * @since 1.0.0
 */
public interface IEventHandler {

    /**
     * 当前处理器支持的事件类型
     * <p>对应钉钉 Stream 推送的 EventType，如 bpms_instance_change、bpms_task_change 等</p>
     *
     * @return 事件类型标识，不可重复
     */
    String supportType();

    /**
     * 处理钉钉事件
     *
     * @param eventId 事件唯一ID
     * @param data   事件原始数据；可能为空
     */
    void handle(String eventId, JSONObject data);
}
