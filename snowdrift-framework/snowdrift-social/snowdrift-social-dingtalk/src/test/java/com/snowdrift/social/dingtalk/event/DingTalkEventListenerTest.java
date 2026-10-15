package com.snowdrift.social.dingtalk.event;

import com.alibaba.fastjson2.JSONObject;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.stream.protocol.event.EventAckStatus;
import com.snowdrift.framework.base.exception.BizException;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class DingTalkEventListenerTest {

    @Test
    void retriesNullEventsAndIgnoresEventsWithoutAType() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("test");
        DingTalkEventListener listener = new DingTalkEventListener(List.of(handler));
        org.mockito.Mockito.clearInvocations(handler);
        assertEquals(EventAckStatus.LATER, listener.onEvent(null));
        assertEquals(EventAckStatus.SUCCESS, listener.onEvent(event(null, "event-1")));
        assertEquals(EventAckStatus.SUCCESS, listener.onEvent(event(" ", "event-2")));
        verifyNoInteractions(handler);
    }

    @Test
    void conversionFailureIsRetriedWithoutCallingTheHandler() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("test");
        DingTalkEventListener listener = new DingTalkEventListener(List.of(handler));
        GenericOpenDingTalkEvent event = event("test", "event-1");
        shade.com.alibaba.fastjson2.JSONObject data = mock(shade.com.alibaba.fastjson2.JSONObject.class);
        when(data.toJSONString()).thenReturn("not-json");
        when(event.getData()).thenReturn(data);
        org.mockito.Mockito.clearInvocations(handler);
        assertEquals(EventAckStatus.LATER, listener.onEvent(event));
        verifyNoInteractions(handler);
    }

    @Test
    void dispatchesRegisteredEventAndAcknowledgesSuccess() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("bpms_instance_change");
        GenericOpenDingTalkEvent event = event("bpms_instance_change", "event-1");

        DingTalkEventListener listener = new DingTalkEventListener(List.of(handler));

        assertEquals(EventAckStatus.SUCCESS, listener.onEvent(event));
        verify(handler).handle("event-1", null);
    }

    @Test
    void convertsSdkJsonToProjectFastjson2Json() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("bpms_instance_change");
        GenericOpenDingTalkEvent event = event("bpms_instance_change", "event-2");
        shade.com.alibaba.fastjson2.JSONObject sdkData = new shade.com.alibaba.fastjson2.JSONObject();
        sdkData.put("processInstanceId", "instance-1");
        when(event.getData()).thenReturn(sdkData);

        DingTalkEventListener listener = new DingTalkEventListener(List.of(handler));

        assertEquals(EventAckStatus.SUCCESS, listener.onEvent(event));
        ArgumentCaptor<JSONObject> dataCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(handler).handle(eq("event-2"), dataCaptor.capture());
        assertEquals("instance-1", dataCaptor.getValue().getString("processInstanceId"));
    }

    @Test
    void retriesWhenHandlerFails() {
        IEventHandler handler = mock(IEventHandler.class);
        when(handler.supportType()).thenReturn("bpms_instance_change");
        doThrow(new BizException("handler failed")).when(handler).handle("event-1", null);

        DingTalkEventListener listener = new DingTalkEventListener(List.of(handler));

        assertEquals(EventAckStatus.LATER, listener.onEvent(event("bpms_instance_change", "event-1")));
    }

    @Test
    void acknowledgesUnknownEventWithoutRetry() {
        DingTalkEventListener listener = new DingTalkEventListener(List.of());

        assertEquals(EventAckStatus.SUCCESS, listener.onEvent(event("unknown_event", "event-1")));
    }

    @Test
    void rejectsDuplicateAndBlankEventTypes() {
        IEventHandler first = mock(IEventHandler.class);
        IEventHandler second = mock(IEventHandler.class);
        when(first.supportType()).thenReturn("same_event");
        when(second.supportType()).thenReturn("same_event");
        assertThrows(BizException.class, () -> new DingTalkEventListener(List.of(first, second)));

        IEventHandler blankType = mock(IEventHandler.class);
        when(blankType.supportType()).thenReturn(" ");
        assertThrows(BizException.class, () -> new DingTalkEventListener(List.of(blankType)));
        assertThrows(BizException.class, () -> new DingTalkEventListener(null));
    }

    @Test
    void autoConfigurationIsRegisteredForSpringBootDiscovery() {
        String resourceName = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(inputStream);
            String imports = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(imports.lines().map(String::trim).anyMatch(
                    "com.snowdrift.social.dingtalk.config.SnowdriftDingTalkConfiguration"::equals));
        } catch (IOException e) {
            throw new BizException("读取钉钉自动配置注册文件失败", e);
        }
    }

    private GenericOpenDingTalkEvent event(String eventType, String eventId) {
        GenericOpenDingTalkEvent event = mock(GenericOpenDingTalkEvent.class);
        when(event.getEventType()).thenReturn(eventType);
        when(event.getEventId()).thenReturn(eventId);
        return event;
    }
}
