package com.snowdrift.social.dingtalk.service;

import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;

/**
 * 稳定的服务门面。每次调用时解析当前激活策略，兼容原有 IDingTalkService 注入方式。
 */
public class ActiveDingTalkService implements IDingTalkService {

    private final DingTalkServiceRegistry registry;

    public ActiveDingTalkService(DingTalkServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public AppAccessTokenVO getAppAccessToken() {
        return registry.getActiveService().getAppAccessToken();
    }

    @Override
    public String getUserOauthUrl(String state) {
        return registry.getActiveService().getUserOauthUrl(state);
    }

    @Override
    public UserAccessTokenVO getUserAccessToken(String authCode) {
        return registry.getActiveService().getUserAccessToken(authCode);
    }
}
