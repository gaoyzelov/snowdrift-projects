package com.snowdrift.social.dingtalk.service;

import com.aliyun.dingtalkoauth2_1_0.models.*;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.social.dingtalk.config.DingTalkServiceConfig;
import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialProperties;
import com.snowdrift.social.dingtalk.utils.ClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;

/**
 *
 * @author 83674
 * @date 2026/10/14-14:08
 * @description
 * @since 1.0.0
 */
@Slf4j
public class IDingTalkServiceImpl implements IDingTalkService {

    private final DingTalkServiceConfig config;

    /**
     * 兼容直接使用旧单配置模型的调用方。
     */
    public IDingTalkServiceImpl(DingTalkSocialProperties properties) {
        this(properties == null ? null : new DingTalkServiceConfig(
                "default",
                properties.getAppKey(),
                properties.getAppSecret(),
                properties.getOauthRedirectUri(),
                true
        ));
    }

    public IDingTalkServiceImpl(DingTalkServiceConfig config) {
        if (config == null) {
            throw new BizException("钉钉服务配置不能为空");
        }
        this.config = config;
    }

    @Override
    public AppAccessTokenVO getAppAccessToken() {
        AssertUtil.notBlank(config.getAppKey(), "钉钉appKey不能为空");
        AssertUtil.notBlank(config.getAppSecret(), "钉钉appSecret不能为空");
        GetAccessTokenRequest request = new GetAccessTokenRequest();
        request.setAppKey(config.getAppKey());
        request.setAppSecret(config.getAppSecret());
        try {
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient = ClientUtil.getInstance(com.aliyun.dingtalkoauth2_1_0.Client.class);
            GetAccessTokenResponse response = oauthClient.getAccessToken(request);
            if (response == null || !Integer.valueOf(200).equals(response.getStatusCode()) || response.getBody() == null) {
                throw new BizException("钉钉应用 Token 响应无效");
            }
            GetAccessTokenResponseBody data = response.getBody();
            validateToken(data.getAccessToken(), data.getExpireIn());
            return new AppAccessTokenVO(data.getAccessToken(), data.getExpireIn());
        } catch (Exception e) {
            throw tokenFailure("获取钉钉应用 AccessToken 失败", e);
        }
    }

    @Override
    public String getUserOauthUrl(String state) {
        AssertUtil.notBlank(config.getAppKey(), "钉钉appKey不能为空");
        validateRedirectUri();
        try {
            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("https")
                    .host("login.dingtalk.com")
                    .addPathSegments("oauth2/auth")
                    .addQueryParameter("redirect_uri", config.getOauthRedirectUri())
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("client_id", config.getAppKey())
                    .addQueryParameter("scope", "openid corpid")
                    .addQueryParameter("prompt", "consent");
            if (StringUtils.isNotBlank(state)) {
                builder.addQueryParameter("state", state);
            }
            return builder.build().toString();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("生成钉钉用户授权 URL 失败", e);
        }
    }

    /**
     * 获取AccessToken
     *
     * @param authCode 授权码
     */
    @Override
    public UserAccessTokenVO getUserAccessToken(String authCode) {
        AssertUtil.notBlank(authCode, "授权码不能为空");
        AssertUtil.notBlank(config.getAppKey(), "钉钉appKey不能为空");
        AssertUtil.notBlank(config.getAppSecret(), "钉钉appSecret不能为空");
        GetUserTokenRequest request = new GetUserTokenRequest()
                .setClientId(config.getAppKey())
                .setClientSecret(config.getAppSecret())
                .setCode(authCode)
                .setGrantType("authorization_code");
        try {
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient = ClientUtil.getInstance(com.aliyun.dingtalkoauth2_1_0.Client.class);
            GetUserTokenResponse response = oauthClient.getUserToken(request);
            if (response == null || !Integer.valueOf(200).equals(response.getStatusCode()) || response.getBody() == null) {
                throw new BizException("钉钉用户 Token 响应无效");
            }
            GetUserTokenResponseBody data = response.getBody();
            validateToken(data.getAccessToken(), data.getExpireIn());
            return new UserAccessTokenVO(data.getAccessToken(), data.getRefreshToken(), data.getCorpId(), data.getExpireIn());
        } catch (Exception e) {
            throw tokenFailure("获取钉钉用户 AccessToken 失败", e);
        }
    }

    private void validateRedirectUri() {
        AssertUtil.notBlank(config.getOauthRedirectUri(), "钉钉 OAuth 回调地址不能为空");
        try {
            URI uri = URI.create(config.getOauthRedirectUri());
            if ((!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    || StringUtils.isBlank(uri.getHost())) {
                throw new IllegalArgumentException("Invalid redirect URI");
            }
        } catch (IllegalArgumentException e) {
            throw new BizException("钉钉 OAuth 回调地址必须为带主机名的绝对 HTTP/HTTPS 地址");
        }
    }

    private void validateToken(String accessToken, Long expireIn) {
        if (StringUtils.isBlank(accessToken) || expireIn == null || expireIn <= 0) {
            throw new BizException("钉钉 Token 或有效期无效");
        }
    }

    private BizException tokenFailure(String operation, Exception cause) {
        log.error("{}, configKey={}, exceptionType={}", operation, config.getConfigKey(), cause.getClass().getName());
        return new BizException(operation, cause);
    }
}
