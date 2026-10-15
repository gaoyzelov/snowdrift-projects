package com.snowdrift.social.dingtalk.event;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dingtalk.open.app.api.GenericEventListener;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DingTalkEventListener
 * @author gaoyzelov
 * @date 2026/10/13-17:33
 * @description 钉钉事件监听器
 * @since 1.0.0
 */
@Slf4j
public class DingTalkEventListener implements GenericEventListener {

    private final Map<String, IEventHandler> handlerMap;

    public DingTalkEventListener(List<IEventHandler> handlers) {
        AssertUtil.notNull(handlers, "钉钉事件处理器列表不能为空");
        Map<String, IEventHandler> registeredHandlers = new LinkedHashMap<>();
        for (IEventHandler handler : handlers) {
            AssertUtil.notNull(handler, "钉钉事件处理器不能为空");
            String eventType = handler.supportType();
            AssertUtil.notBlank(eventType, "钉钉事件类型不能为空");
            IEventHandler registeredHandler = registeredHandlers.putIfAbsent(eventType, handler);
            if (registeredHandler != null) {
                throw new BizException(
                        "重复的钉钉事件处理器, eventType=" + eventType
                                + ", 已注册=" + registeredHandler.getClass().getSimpleName()
                                + ", 冲突=" + handler.getClass().getSimpleName());
            }
            log.info("[钉钉事件] 注册处理器: {} -> {}", eventType, handler.getClass().getSimpleName());
        }
        this.handlerMap = Map.copyOf(registeredHandlers);
    }

    @Override
    public EventAckStatus onEvent(GenericOpenDingTalkEvent event) {
        if (event == null) {
            log.error("[钉钉事件] 收到空事件，稍后重试");
            return EventAckStatus.LATER;
        }
        String eventType = event.getEventType();
        String eventId = event.getEventId();
        if (StringUtils.isBlank(eventType)) {
            log.warn("[钉钉事件] 事件类型为空，确认并忽略, eventId={}", eventId);
            return EventAckStatus.SUCCESS;
        }
        IEventHandler handler = handlerMap.get(eventType);
        if (handler == null) {
            log.warn("[钉钉事件] 未找到处理器, eventType={}, eventId={}", eventType, eventId);
            return EventAckStatus.SUCCESS;
        }
        try {
            handler.handle(eventId, convertEventData(event.getData()));
            return EventAckStatus.SUCCESS;
        } catch (Exception e) {
            log.error("[钉钉事件] 处理失败, eventType={}, eventId={}", eventType, eventId, e);
            return EventAckStatus.LATER;
        }
    }

    private JSONObject convertEventData(shade.com.alibaba.fastjson2.JSONObject eventData) {
        if (eventData == null) {
            return null;
        }
        try {
            JSONObject convertedData = JSON.parseObject(eventData.toJSONString());
            if (convertedData == null) {
                throw new BizException("钉钉事件数据转换结果不能为空");
            }
            return convertedData;
        } catch (Exception e) {
            throw new BizException("钉钉事件数据转换失败", e);
        }
    }
}
