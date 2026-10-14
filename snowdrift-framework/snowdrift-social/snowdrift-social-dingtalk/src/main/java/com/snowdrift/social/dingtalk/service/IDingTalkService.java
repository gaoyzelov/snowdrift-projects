package com.snowdrift.social.dingtalk.service;

import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;

/**
 *
 * @author 83674
 * @date 2026/10/14-14:08
 * @description
 * @since 1.0.0
 */
public interface IDingTalkService {

    /**
     * 获取企业内部应用的accessToken
     *
     * @return {@link AppAccessTokenVO}
     */
    AppAccessTokenVO getAppAccessToken();

    /**
     * 获取用户授权url
     * @param state 跟随 authCode 原样返回，选填；下游应生成并校验一次性授权状态，关联原 configKey
     * @return 授权url
     */
    String getUserOauthUrl(String state);

    /**
     * 获取用户accessToken
     *
     * @param authCode 授权码
     * @return {@link UserAccessTokenVO}
     */
    UserAccessTokenVO getUserAccessToken(String authCode);
}
