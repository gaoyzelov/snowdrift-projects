package com.snowdrift.framework.schedule.xxljob.service;

import com.snowdrift.framework.base.util.HttpUtil;
import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.schedule.xxljob.config.XxlJobProperties;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobKey;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * XxlJobLoginExpiryDetectionTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description SCH-04：「登录过期」的判定必须收窄。把 Admin 的<b>任何</b>业务失败都当成会话失效，
 *              会导致每次业务报错都强制重登并<b>重放同一次请求</b>（对非幂等操作可能重复执行），
 *              同时把真实错误掩盖成「登录过期」
 * @since 1.0.0
 */
class XxlJobLoginExpiryDetectionTest {

    private MockedStatic<HttpUtil> mockedHttp;
    private HttpClient httpClient;
    private XxlJobScheduleServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        mockedHttp = mockStatic(HttpUtil.class);

        httpClient = mock(HttpClient.class);
        HttpResponse<String> loginResp = mock(HttpResponse.class);
        when(loginResp.body()).thenReturn("{\"code\":200}");
        when(loginResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("Set-Cookie", List.of("xxl_job_login_token=abc123")), (a, b) -> true));
        doReturn(loginResp).when(httpClient).send(any(), any());
        mockedHttp.when(HttpUtil::getHttpClient).thenReturn(httpClient);
        mockedHttp.when(() -> HttpUtil.buildFormDataBody(any())).thenReturn("userName=admin&password=x");
        mockedHttp.when(() -> HttpUtil.buildUrlWithParams(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        mockedHttp.when(() -> HttpUtil.get(any(), any(), any(), anyLong()))
                .thenReturn("{\"code\":200,\"data\":{\"data\":[{\"id\":7,\"appname\":\"snowdrift-job\"}]}}");
        mockedHttp.when(() -> HttpUtil.postForm(any(), any(), any(), any())).thenReturn("{\"code\":200}");

        service = new XxlJobScheduleServiceImpl(props());
    }

    @AfterEach
    void tearDown() {
        if (mockedHttp != null) {
            mockedHttp.close();
        }
    }

    private XxlJobProperties props() {
        XxlJobProperties p = new XxlJobProperties();
        p.setAdminAddresses("http://admin");
        p.setAppName("snowdrift-job");
        p.setUsername("admin");
        p.setPassword("123456");
        return p;
    }

    private XxlJobRequest request() {
        XxlJobRequest req = new XxlJobRequest();
        req.setName("demoHandler");
        req.setCron("0 0 2 * * ?");
        req.setGroup("snowdrift-job");
        return req;
    }

    @Test
    @DisplayName("业务错误（cron 非法）：按业务错误抛出，不得强制重登、更不得重放请求")
    void businessErrorIsNotTreatedAsSessionExpiry() throws Exception {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()))
                .thenReturn("{\"code\":500,\"msg\":\"cron 表达式非法\"}");

        assertThatThrownBy(() -> service.updateJob(XxlJobKey.newInstance(42, 7), request()))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("cron 表达式非法");

        // 旧实现会把这条判成「登录过期」→ 强制重登 + 重放一次 update（非幂等操作会被重复执行）
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()), times(1));
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("会话失效（容器默认错误页，非 JSON）：重登并重试一次后成功")
    void htmlErrorPageTriggersReloginAndOneRetry() throws Exception {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()))
                .thenReturn("<html><head><title>XXL-JOB</title></head><body>请登录</body></html>")
                .thenReturn("{\"code\":200}");

        // 旧实现把 HTML 丢给 JSON 解析器 → 抛异常 → 被当成「管理地址不可达」，真实到期反而无人处理
        assertThatCode(() -> service.updateJob(XxlJobKey.newInstance(42, 7), request()))
                .doesNotThrowAnyException();

        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()), times(2));
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @DisplayName("会话失效（容器默认错误响应的 JSON，不带 code 字段）：同样触发重登并重试一次")
    void containerErrorJsonTriggersRelogin() {
        // 未登录时 XxlSsoWebInterceptor 对 @ResponseBody 端点抛 XxlSsoException，
        // 而 admin 与 xxl-sso 都没有注册该异常的处理器 → 落到容器默认错误响应；
        // 内容协商选中 JSON 时就是这种形态（没有 code 字段，因此不是 Admin API 的响应）。
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()))
                .thenReturn("{\"timestamp\":\"2026-10-12T00:00:00.000+00:00\",\"status\":500,"
                        + "\"error\":\"Internal Server Error\",\"path\":\"/jobinfo/update\"}")
                .thenReturn("{\"code\":200}");

        assertThatCode(() -> service.updateJob(XxlJobKey.newInstance(42, 7), request()))
                .doesNotThrowAnyException();

        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/update"), any(), any(), any()), times(2));
    }

}
