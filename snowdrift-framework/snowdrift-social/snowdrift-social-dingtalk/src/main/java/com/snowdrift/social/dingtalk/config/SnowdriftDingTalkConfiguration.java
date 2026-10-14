package com.snowdrift.social.dingtalk.config;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.social.dingtalk.event.DingTalkEventListener;
import com.snowdrift.social.dingtalk.event.IEventHandler;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import com.snowdrift.social.dingtalk.service.IDingTalkService;
import com.snowdrift.social.dingtalk.service.ActiveDingTalkService;
import com.snowdrift.social.dingtalk.service.DingTalkServiceRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * SnowdriftDingTalkConfiguration
 * @author gaoyzelov
 * @date 2026/10/13-17:21
 * @description 钉钉社交自动配置
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DingTalkSocialProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.social.dingtalk", name = "enabled", havingValue = "true")
public class SnowdriftDingTalkConfiguration {

    private OpenDingTalkClient openDingTalkClient;

    @Bean(name = "openDingTalkClient", destroyMethod = "")
    @ConditionalOnProperty(prefix = "snowdrift.social.dingtalk.event-listener", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(OpenDingTalkClient.class)
    public OpenDingTalkClient createOpenDingTalkClient(DingTalkSocialProperties properties, ObjectProvider<IEventHandler> handlers) {
        return openDingTalkClient(properties, handlers.orderedStream().toList());
    }

    public OpenDingTalkClient openDingTalkClient(DingTalkSocialProperties properties, List<IEventHandler> eventHandlers) {
        AssertUtil.notNull(eventHandlers, "钉钉事件处理器列表不能为空");
        if (eventHandlers.isEmpty()) {
            throw new BizException("启用钉钉 Stream 时必须提供 IEventHandler Bean");
        }
        DingTalkSocialProperties.EventListenerProperties eventListener = properties.getEventListener();
        AssertUtil.notNull(eventListener, "钉钉流监听配置不能为空");
        AssertUtil.notBlank(eventListener.getClientId(), "钉钉流监听配置的 clientId 不能为空");
        AssertUtil.notBlank(eventListener.getClientSecret(), "钉钉流监听配置的 clientSecret 不能为空");
        OpenDingTalkClient dingTalkClient = null;
        try {
            dingTalkClient = OpenDingTalkStreamClientBuilder
                    .custom()
                    .credential(new AuthClientCredential(eventListener.getClientId(), eventListener.getClientSecret()))
                    .registerAllEventListener(new DingTalkEventListener(eventHandlers))
                    .build();
            dingTalkClient.start();
        } catch (Exception e) {
            if (dingTalkClient != null) {
                try {
                    dingTalkClient.stop();
                } catch (Exception stopException) {
                    e.addSuppressed(stopException);
                }
            }
            throw new BizException("钉钉 Stream 客户端启动失败", e);
        }
        openDingTalkClient = dingTalkClient;
        log.info("[钉钉Stream] 事件监听已启动, 已注册 {} 个处理器", eventHandlers.size());
        return openDingTalkClient;
    }

    @Bean
    @ConditionalOnMissingBean(IDingTalkConfigProvider.class)
    public IDingTalkConfigProvider propertiesDingTalkConfigProvider(DingTalkSocialProperties properties) {
        return new PropertiesDingTalkConfigProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean({DingTalkServiceRegistry.class, IDingTalkService.class})
    public DingTalkServiceRegistry dingTalkServiceRegistry(IDingTalkConfigProvider configProvider) {
        return new DingTalkServiceRegistry(configProvider);
    }

    @Bean
    @ConditionalOnMissingBean(IDingTalkService.class)
    public IDingTalkService defaultDingTalkService(DingTalkServiceRegistry registry) {
        return new ActiveDingTalkService(registry);
    }

    @PreDestroy
    public synchronized void close() {
        OpenDingTalkClient client = openDingTalkClient;
        openDingTalkClient = null;
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                throw new BizException("钉钉 Stream 客户端停止失败", e);
            }
        }
    }
}
