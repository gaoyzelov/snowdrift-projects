package com.snowdrift.social.dingtalk.service;

import com.aliyun.dingtalkcontact_1_0.models.GetUserHeaders;
import com.aliyun.dingtalkcontact_1_0.models.GetUserResponse;
import com.aliyun.dingtalkcontact_1_0.models.GetUserResponseBody;
import com.aliyun.dingtalkoauth2_1_0.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserGetbyunionidRequest;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.response.OapiUserGetbyunionidResponse;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import com.snowdrift.framework.base.util.ValidateUtil;
import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserInfoVO;
import com.snowdrift.social.dingtalk.model.UserDetailVO;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import com.snowdrift.social.dingtalk.utils.DingTalkClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

/**
 * DingTalkServiceImpl
 *
 * @author gaoyzelov
 * @date 2026/10/14-14:08
 * @description 钉钉服务实现类
 * @since 1.0.0
 */
@Slf4j
public class DingTalkServiceImpl implements IDingTalkService {

    private final DingTalkSocialConfig config;

    public DingTalkServiceImpl(DingTalkSocialConfig config) {
        if (config == null) {
            throw new BizException("钉钉服务配置不能为空");
        }
        this.config = new DingTalkSocialConfig(config.getConfigKey(), config.getAppKey(),
                config.getAppSecret(), config.getOauthRedirectUri());
        AssertUtil.notBlank(this.config.getAppKey(), "钉钉appKey不能为空");
        AssertUtil.notBlank(this.config.getAppSecret(), "钉钉appSecret不能为空");
    }

    @Override
    public AppAccessTokenVO getAppAccessToken() {
        AssertUtil.notBlank(config.getAppKey(), "钉钉appKey不能为空");
        AssertUtil.notBlank(config.getAppSecret(), "钉钉appSecret不能为空");
        GetAccessTokenRequest request = new GetAccessTokenRequest();
        request.setAppKey(config.getAppKey());
        request.setAppSecret(config.getAppSecret());
        try {
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient = DingTalkClientUtil.getInstance(com.aliyun.dingtalkoauth2_1_0.Client.class);
            GetAccessTokenResponse response = oauthClient.getAccessToken(request);
            if (response == null || !Integer.valueOf(200).equals(response.getStatusCode()) || response.getBody() == null) {
                throw new BizException("钉钉应用 Token 响应无效");
            }
            GetAccessTokenResponseBody data = response.getBody();
            return AppAccessTokenVO.builder()
                    .accessToken(data.getAccessToken())
                    .expiresIn(data.getExpireIn())
                    .build();
        } catch (Exception e) {
            log.error("[钉钉认证] 获取应用 AccessToken 失败", e);
            throw new BizException("获取钉钉应用 AccessToken 失败", e);
        }
    }

    @Override
    public String getUserOauthUrl(String state) {
        AssertUtil.notBlank(config.getAppKey(), "钉钉appKey不能为空");
        if (!ValidateUtil.isURL(config.getOauthRedirectUri())) {
            throw new BizException("钉钉 OAuth 回调地址有误");
        }
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
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient = DingTalkClientUtil.getInstance(com.aliyun.dingtalkoauth2_1_0.Client.class);
            GetUserTokenResponse response = oauthClient.getUserToken(request);
            if (response == null || !Integer.valueOf(200).equals(response.getStatusCode()) || response.getBody() == null) {
                throw new BizException("钉钉用户 Token 响应无效");
            }
            GetUserTokenResponseBody data = response.getBody();
            return UserAccessTokenVO.builder()
                    .accessToken(data.getAccessToken())
                    .refreshToken(data.getRefreshToken())
                    .corpId(data.getCorpId())
                    .expireIn(data.getExpireIn())
                    .build();
        } catch (Exception e) {
            log.error("[钉钉认证] 获取用户 AccessToken 失败", e);
            throw new BizException("获取钉钉用户 AccessToken 失败", e);
        }
    }

    /**
     * 获取用户信息
     *
     * @param accessToken 访问令牌
     */
    @Override
    public UserInfoVO getUserInfo(String accessToken) {
        AssertUtil.notBlank(accessToken, "用户访问令牌不能为空");
        GetUserHeaders headers = new GetUserHeaders().setXAcsDingtalkAccessToken(accessToken);
        try {
            com.aliyun.dingtalkcontact_1_0.Client contactClient = DingTalkClientUtil.getInstance(com.aliyun.dingtalkcontact_1_0.Client.class);
            GetUserResponse response = contactClient.getUserWithOptions("me", headers, new RuntimeOptions());
            if (response.getStatusCode() != 200) {
                log.error("[钉钉认证] 获取用户信息失败,statusCode={}", response.getStatusCode());
                throw new BizException("钉钉接口返回异常状态码: " + response.getStatusCode());
            }
            GetUserResponseBody data = response.getBody();
            return UserInfoVO.builder()
                    .avatarUrl(data.getAvatarUrl())
                    .email(data.getEmail())
                    .mobile(data.getMobile())
                    .nick(data.getNick())
                    .openId(data.getOpenId())
                    .stateCode(data.getStateCode())
                    .unionId(data.getUnionId())
                    .build();
        } catch (BizException e) {
            log.error("[钉钉认证] 获取用户信息失败", e);
            throw e;
        } catch (Exception e) {
            log.error("[钉钉认证] 获取用户信息失败", e);
            throw new BizException("获取用户信息失败");
        }
    }

    /**
     * 获取用户ID
     *
     * @param accessToken 应用访问令牌
     * @param unionId     用户的unionId
     */
    @Override
    public String getUserId(String accessToken, String unionId) {
        AssertUtil.notBlank(accessToken, "应用访问令牌不能为空");
        AssertUtil.notBlank(unionId, "用户unionId不能为空");
        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/user/getbyunionid");
            OapiUserGetbyunionidRequest request = new OapiUserGetbyunionidRequest();
            request.setUnionid(unionId);
            OapiUserGetbyunionidResponse response = client.execute(request, accessToken);
            if (!response.isSuccess()) {
                log.error("[钉钉认证] 获取用户ID失败, statusCode={}", response.getErrorCode());
                throw new BizException("钉钉接口返回异常状态码: " + response.getErrorCode());
            }
            return response.getResult().getUserid();
        } catch (BizException e) {
            log.error("[钉钉认证] 获取用户ID失败", e);
            throw e;
        } catch (Exception e) {
            log.error("[钉钉认证] 获取用户ID失败", e);
            throw new BizException("获取用户ID失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     *
     * @param accessToken 应用访问令牌
     * @param userId      用户ID
     */
    @Override
    public UserDetailVO getUserDetail(String accessToken, String userId) {
        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get");
            OapiV2UserGetRequest request = new OapiV2UserGetRequest();
            request.setUserid(userId);
            OapiV2UserGetResponse response = client.execute(request, accessToken);
            if (!response.isSuccess()) {
                log.error("[钉钉认证] 获取用户详情失败, statusCode={}, errorMsg={}", response.getErrorCode(), response.getErrmsg());
                throw new BizException("钉钉接口返回异常状态码: " + response.getErrorCode());
            }
            OapiV2UserGetResponse.UserGetResponse result = response.getResult();
            // TODO
            return null;
        }  catch (BizException e){
            log.error("[钉钉认证] 获取用户详情失败", e);
            throw e;
        }catch (Exception e) {
            log.error("[钉钉认证] 获取用户详情失败", e);
            throw new BizException("获取用户详情失败: " + e.getMessage());
        }
    }
}
