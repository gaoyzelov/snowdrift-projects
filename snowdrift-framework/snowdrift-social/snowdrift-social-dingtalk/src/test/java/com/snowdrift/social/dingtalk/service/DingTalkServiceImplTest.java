package com.snowdrift.social.dingtalk.service;

import com.aliyun.dingtalkoauth2_1_0.Client;
import com.aliyun.dingtalkoauth2_1_0.models.*;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;
import com.snowdrift.social.dingtalk.utils.DingTalkClientUtil;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DingTalkServiceImplTest {

    private IDingTalkService service(String redirect) {
        return new DingTalkServiceImpl(new DingTalkSocialConfig("primary", "test-app", "test-secret", redirect));
    }

    @Test
    void serviceCopiesCredentialsInsteadOfRetainingMutableInput() throws Exception {
        DingTalkSocialConfig config = new DingTalkSocialConfig("primary", "original-app", "original-secret", null);
        IDingTalkService service = new DingTalkServiceImpl(config);
        config.setAppKey("changed-app");
        config.setAppSecret("changed-secret");
        Client client = mock(Client.class);
        when(client.getAccessToken(any())).thenReturn(appResponse("access", 7200L));
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            clients.when(() -> DingTalkClientUtil.getInstance(Client.class)).thenReturn(client);
            service.getAppAccessToken();
            ArgumentCaptor<GetAccessTokenRequest> request = ArgumentCaptor.forClass(GetAccessTokenRequest.class);
            verify(client).getAccessToken(request.capture());
            assertThat(request.getValue().getAppKey()).isEqualTo("original-app");
            assertThat(request.getValue().getAppSecret()).isEqualTo("original-secret");
        }
    }

    @Test
    void buildsUrlWithRoundTripEncoding() {
        String redirect = "https://example.com/callback?next=a%20b&x=1";
        String state = "state +&=/中文";
        HttpUrl url = HttpUrl.parse(service(redirect).getUserOauthUrl(state));
        assertThat(url).isNotNull();
        assertThat(url.queryParameter("redirect_uri")).isEqualTo(redirect);
        assertThat(url.queryParameter("state")).isEqualTo(state);
        assertThat(url.queryParameter("client_id")).isEqualTo("test-app");
        assertThat(url.queryParameter("response_type")).isEqualTo("code");
        assertThat(url.queryParameter("scope")).isEqualTo("openid corpid");
        assertThat(HttpUrl.parse(service("http://localhost:8080/callback").getUserOauthUrl(null))
                .queryParameter("state")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "/callback", "//example.com/callback", "ftp://example.com/callback", "https:///callback", "https://exa mple.com/callback"})
    void rejectsInvalidRedirectAtCallTime(String redirect) {
        IDingTalkService service = service(redirect);
        assertThatThrownBy(() -> service.getUserOauthUrl(null)).isInstanceOf(BizException.class);
    }

    @Test
    void validatesInputBeforeCallingSdk() {
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            assertThatThrownBy(() -> service(null).getUserAccessToken(" ")).isInstanceOf(BizException.class);
            assertThatThrownBy(() -> new DingTalkServiceImpl(null)).isInstanceOf(BizException.class);
            assertThatThrownBy(() -> new DingTalkServiceImpl(new DingTalkSocialConfig("a", "", "secret", null)))
                    .isInstanceOf(BizException.class).hasMessage("钉钉appKey不能为空");
            assertThatThrownBy(() -> new DingTalkServiceImpl(new DingTalkSocialConfig("a", "app", "", null)))
                    .isInstanceOf(BizException.class).hasMessage("钉钉appSecret不能为空");
            clients.verifyNoInteractions();
        }
    }

    @Test
    void mapsResponsesAndSendsCredentialsWithoutRequiringRedirect() throws Exception {
        Client client = mock(Client.class);
        when(client.getAccessToken(any())).thenReturn(appResponse("access", 7200L));
        when(client.getUserToken(any())).thenReturn(new GetUserTokenResponse().setStatusCode(200)
                .setBody(new GetUserTokenResponseBody().setAccessToken("user-access").setExpireIn(3600L)
                        .setRefreshToken("refresh").setCorpId("corp")));
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            clients.when(() -> DingTalkClientUtil.getInstance(Client.class)).thenReturn(client);
            IDingTalkService service = service(null);
            AppAccessTokenVO app = service.getAppAccessToken();
            assertThat(app.getAccessToken()).isEqualTo("access");
            assertThat(app.getExpiresIn()).isEqualTo(7200L);
            UserAccessTokenVO user = service.getUserAccessToken("auth-code");
            assertThat(user.getAccessToken()).isEqualTo("user-access");
            assertThat(user.getRefreshToken()).isEqualTo("refresh");
            assertThat(user.getCorpId()).isEqualTo("corp");
            assertThat(user.getExpireIn()).isEqualTo(3600L);
            ArgumentCaptor<GetAccessTokenRequest> appRequest = ArgumentCaptor.forClass(GetAccessTokenRequest.class);
            verify(client).getAccessToken(appRequest.capture());
            assertThat(appRequest.getValue().getAppKey()).isEqualTo("test-app");
            assertThat(appRequest.getValue().getAppSecret()).isEqualTo("test-secret");
            ArgumentCaptor<GetUserTokenRequest> userRequest = ArgumentCaptor.forClass(GetUserTokenRequest.class);
            verify(client).getUserToken(userRequest.capture());
            assertThat(userRequest.getValue().getClientId()).isEqualTo("test-app");
            assertThat(userRequest.getValue().getClientSecret()).isEqualTo("test-secret");
            assertThat(userRequest.getValue().getCode()).isEqualTo("auth-code");
            assertThat(userRequest.getValue().getGrantType()).isEqualTo("authorization_code");
        }
    }

    @Test
    void rejectsMalformedAppAndUserResponses() throws Exception {
        Client client = mock(Client.class);
        GetAccessTokenResponse[] invalid = {null, new GetAccessTokenResponse(),
                new GetAccessTokenResponse().setStatusCode(500), new GetAccessTokenResponse().setStatusCode(200),
                appResponse(null, 3600L), appResponse(" ", 3600L), appResponse("access", null),
                appResponse("access", 0L), appResponse("access", -1L)};
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            clients.when(() -> DingTalkClientUtil.getInstance(Client.class)).thenReturn(client);
            for (GetAccessTokenResponse response : invalid) {
                when(client.getAccessToken(any())).thenReturn(response);
                assertThatThrownBy(service(null)::getAppAccessToken).isInstanceOf(BizException.class)
                        .hasMessage("获取钉钉应用 AccessToken 失败").hasCauseInstanceOf(BizException.class);
                GetUserTokenResponse userResponse = response == null ? null
                        : new GetUserTokenResponse().setStatusCode(response.getStatusCode());
                if (response != null && response.getBody() != null) {
                    userResponse.setBody(new GetUserTokenResponseBody().setAccessToken(response.getBody().getAccessToken())
                            .setExpireIn(response.getBody().getExpireIn()));
                }
                when(client.getUserToken(any())).thenReturn(userResponse);
                assertThatThrownBy(() -> service(null).getUserAccessToken("code")).isInstanceOf(BizException.class)
                        .hasMessage("获取钉钉用户 AccessToken 失败").hasCauseInstanceOf(BizException.class);
            }
        }
    }

    @Test
    void preservesSdkCauseWithoutExposingItsMessageOrLoggingSecrets() throws Exception {
        Client client = mock(Client.class);
        Exception sdkFailure = new Exception("test-secret auth-code access refresh");
        when(client.getAccessToken(any())).thenThrow(sdkFailure);
        when(client.getUserToken(any())).thenThrow(sdkFailure);
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(DingTalkServiceImpl.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs = new ch.qos.logback.core.read.ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            clients.when(() -> DingTalkClientUtil.getInstance(Client.class)).thenReturn(client);
            assertThatThrownBy(service(null)::getAppAccessToken).isInstanceOf(BizException.class)
                    .hasMessage("获取钉钉应用 AccessToken 失败").hasCause(sdkFailure);
            assertThatThrownBy(() -> service(null).getUserAccessToken("auth-code")).isInstanceOf(BizException.class)
                    .hasMessage("获取钉钉用户 AccessToken 失败").hasCause(sdkFailure);
            assertThat(logs.list).hasSize(2).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("primary").doesNotContain("test-secret", "auth-code", "refresh");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void wrapsClientCreationFailure() {
        RuntimeException failure = new IllegalStateException("secret");
        try (MockedStatic<DingTalkClientUtil> clients = mockStatic(DingTalkClientUtil.class)) {
            clients.when(() -> DingTalkClientUtil.getInstance(Client.class)).thenThrow(failure);
            assertThatThrownBy(service(null)::getAppAccessToken).hasMessage("获取钉钉应用 AccessToken 失败").hasCause(failure);
            assertThatThrownBy(() -> service(null).getUserAccessToken("code")).hasMessage("获取钉钉用户 AccessToken 失败").hasCause(failure);
        }
    }

    @Test
    void tokenToStringIsRedactedWhileJsonRemainsCompatible() {
        AppAccessTokenVO app = new AppAccessTokenVO("fake-access", 7200L);
        UserAccessTokenVO user = new UserAccessTokenVO("fake-user-access", "fake-refresh", "corp", 3600L);
        assertThat(app.toString()).doesNotContain("fake-access");
        assertThat(user.toString()).doesNotContain("fake-user-access", "fake-refresh");
        assertThat(com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(app)).getString("accessToken"))
                .isEqualTo("fake-access");
        assertThat(com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(user)).getString("refreshToken"))
                .isEqualTo("fake-refresh");
    }

    private GetAccessTokenResponse appResponse(String token, Long expires) {
        return new GetAccessTokenResponse().setStatusCode(200)
                .setBody(new GetAccessTokenResponseBody().setAccessToken(token).setExpireIn(expires));
    }
}
