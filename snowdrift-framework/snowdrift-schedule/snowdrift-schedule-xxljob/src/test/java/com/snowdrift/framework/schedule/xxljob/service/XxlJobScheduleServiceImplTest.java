package com.snowdrift.framework.schedule.xxljob.service;

import com.snowdrift.framework.base.util.HttpUtil;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.schedule.model.JobDetails;
import com.snowdrift.framework.schedule.xxljob.config.XxlJobProperties;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobKey;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * XxlJobScheduleServiceImplTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description XXL-JOB 调度实现黑盒测试：仅在 HTTP 边界（HttpUtil 静态方法）打桩，覆盖登录、增删改查、
 * 参数默认值与序列化、响应校验、登录过期重试、分组解析与 AppName 反查降级兜底、异常统一化
 * @since 1.0.0
 */
class XxlJobScheduleServiceImplTest {

    private MockedStatic<HttpUtil> mockedHttp;
    private XxlJobScheduleServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        mockedHttp = mockStatic(HttpUtil.class);

        // ---- 登录桩：doLogin 走 HttpUtil.getHttpClient().send(...) ----
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> loginResp = mock(HttpResponse.class);
        when(loginResp.body()).thenReturn("{\"code\":200}");
        when(loginResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("Set-Cookie", List.of("xxl_job_login_token=abc123")), (a, b) -> true));
        doReturn(loginResp).when(httpClient).send(any(), any());
        mockedHttp.when(HttpUtil::getHttpClient).thenReturn(httpClient);
        mockedHttp.when(() -> HttpUtil.buildFormDataBody(any())).thenReturn("userName=admin&password=x");
        mockedHttp.when(() -> HttpUtil.buildUrlWithParams(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        // ---- 默认 GET（执行器分组分页）：snowdrift-job ↔ id=7 ----
        mockedHttp.when(() -> HttpUtil.get(any(), any(), any(), anyLong()))
                .thenReturn("{\"code\":200,\"data\":{\"data\":[{\"id\":7,\"appname\":\"snowdrift-job\"}]}}");

        // ---- 默认 POST：成功；各测试按需以更具体的 path 匹配覆盖 ----
        mockedHttp.when(() -> HttpUtil.postForm(any(), any(), any(), any())).thenReturn("{\"code\":200}");

        service = new XxlJobScheduleServiceImpl(props("http://admin"));
    }

    @AfterEach
    void tearDown() {
        if (mockedHttp != null) {
            mockedHttp.close();
        }
    }

    private XxlJobProperties props(String adminAddresses) {
        XxlJobProperties p = new XxlJobProperties();
        p.setAdminAddresses(adminAddresses);
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
        req.setDescription("演示任务");
        return req;
    }

    // ========== 分组解析失败：统一异常契约（回归 Guava UncheckedExecutionException 缺陷） ==========

    @Test
    @DisplayName("listJobs：未配置管理地址时抛 ScheduleException 而非 UncheckedExecutionException")
    void listJobsPropagatesScheduleException() {
        XxlJobScheduleServiceImpl noAdmin = new XxlJobScheduleServiceImpl(props(""));

        assertThatThrownBy(noAdmin::listJobs).isInstanceOf(ScheduleException.class);
    }

    @Test
    @DisplayName("addJob：未配置管理地址时抛 ScheduleException 而非 UncheckedExecutionException")
    void addJobPropagatesScheduleException() {
        XxlJobScheduleServiceImpl noAdmin = new XxlJobScheduleServiceImpl(props(""));

        assertThatThrownBy(() -> noAdmin.addJob(request())).isInstanceOf(ScheduleException.class);
    }

    // ========== addJob ==========

    @Test
    @DisplayName("addJob：成功返回 id/groupId，并按默认值构建 Admin 表单参数")
    @SuppressWarnings("unchecked")
    void addJobSuccessBuildsParams() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/insert"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":42}");

        XxlJobKey key = service.addJob(request());

        assertThat(key.getId()).isEqualTo(42);
        assertThat(key.getGroupId()).isEqualTo(7);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/insert"), captor.capture(), any(), any()));
        assertThat(captor.getValue())
                .containsEntry("jobGroup", "7")
                .containsEntry("scheduleType", "CRON")
                .containsEntry("scheduleConf", "0 0 2 * * ?")
                .containsEntry("executorHandler", "demoHandler")
                .containsEntry("jobDesc", "演示任务")
                .containsEntry("glueType", "BEAN")
                .containsEntry("misfireStrategy", "FIRE_ONCE_NOW")
                .containsEntry("executorRouteStrategy", "RANDOM")
                .containsEntry("executorBlockStrategy", "SERIAL_EXECUTION")
                .containsEntry("executorTimeout", "0")
                .containsEntry("executorFailRetryCount", "0")
                .containsEntry("triggerStatus", "1")
                .containsEntry("executorParam", "");
    }

    @Test
    @DisplayName("addJob：name 空白抛 ScheduleException（与 Quartz 一致，非裸 BizException）")
    void addJobRejectsBlankName() {
        XxlJobRequest req = request();
        req.setName("  ");

        assertThatThrownBy(() -> service.addJob(req))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("XXL-JOB 任务名称不能为空");
    }

    @Test
    @DisplayName("addJob：cron 空白抛 ScheduleException")
    void addJobRejectsBlankCron() {
        XxlJobRequest req = request();
        req.setCron(null);

        assertThatThrownBy(() -> service.addJob(req))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("XXL-JOB Cron 表达式不能为空");
    }

    @Test
    @DisplayName("addJob：params 非空时 executorParam 序列化为 JSON")
    @SuppressWarnings("unchecked")
    void addJobSerializesParams() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/insert"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":1}");
        XxlJobRequest req = request();
        req.setParams(Map.of("p", "v"));

        service.addJob(req);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/insert"), captor.capture(), any(), any()));
        assertThat(captor.getValue().get("executorParam")).contains("\"p\"").contains("\"v\"");
    }

    // ========== remove / pause / resume / trigger / update ==========

    @Test
    @DisplayName("removeJob：以 ids[] 调用 delete 接口")
    @SuppressWarnings("unchecked")
    void removeJob() {
        service.removeJob(XxlJobKey.newInstance(42, 7));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/delete"), captor.capture(), any(), any()));
        assertThat(captor.getValue()).containsEntry("ids[]", "42");
    }

    @Test
    @DisplayName("pauseJob / resumeJob：分别调用 stop / start 接口")
    void pauseAndResume() {
        service.pauseJob(XxlJobKey.newInstance(42, 7));
        service.resumeJob(XxlJobKey.newInstance(42, 7));

        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/stop"), any(), any(), any()));
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/start"), any(), any(), any()));
    }

    @Test
    @DisplayName("triggerJob：携带 executorParam 与空 addressList")
    @SuppressWarnings("unchecked")
    void triggerJob() {
        service.triggerJob(XxlJobKey.newInstance(42, 7), Map.of("p", "v"));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/trigger"), captor.capture(), any(), any()));
        assertThat(captor.getValue())
                .containsEntry("id", "42")
                .containsEntry("addressList", "");
        assertThat(captor.getValue().get("executorParam")).contains("\"p\"");
    }

    @Test
    @DisplayName("updateJob：使用 jobKey 的 groupId，携带 id 调用 update 接口")
    @SuppressWarnings("unchecked")
    void updateJob() {
        service.updateJob(XxlJobKey.newInstance(42, 7), request());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/update"), captor.capture(), any(), any()));
        assertThat(captor.getValue()).containsEntry("id", "42").containsEntry("jobGroup", "7");
    }

    // ========== 响应校验 & 登录过期重试 ==========

    @Test
    @DisplayName("API 返回非 200（无 msg）时抛 ScheduleException（API 调用失败）")
    void validateApiResponseFailure() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/delete"), any(), any(), any()))
                .thenReturn("{\"code\":500}");

        assertThatThrownBy(() -> service.removeJob(XxlJobKey.newInstance(42, 7)))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("XXL-JOB API 调用失败");
    }

    @Test
    @DisplayName("登录过期：强制重登并重试一次后成功")
    void loginExpiredRetriesOnce() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/delete"), any(), any(), any()))
                .thenReturn("{\"code\":500,\"msg\":\"登录已过期\"}")
                .thenReturn("{\"code\":200}");

        service.removeJob(XxlJobKey.newInstance(42, 7));

        mockedHttp.verify(() -> HttpUtil.postForm(contains("/jobinfo/delete"), any(), any(), any()), times(2));
    }

    // ========== 查询 & 转换 ==========

    @Test
    @DisplayName("getJob：命中并转换全部字段（状态、cron、参数、触发时间空安全）")
    void getJobFound() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/pageList"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":{\"total\":1,\"data\":[{"
                        + "\"id\":42,\"jobGroup\":7,\"executorHandler\":\"demoHandler\","
                        + "\"scheduleConf\":\"0 0 2 * * ?\",\"jobDesc\":\"演示\",\"triggerStatus\":1,"
                        + "\"executorParam\":\"{\\\"p\\\":\\\"v\\\"}\","
                        + "\"triggerLastTime\":1600000000000,\"triggerNextTime\":0}]}}");

        JobDetails details = service.getJob(XxlJobKey.newInstance(42, 7));

        assertThat(details).isNotNull();
        assertThat(details.getName()).isEqualTo("demoHandler");
        assertThat(details.getCron()).isEqualTo("0 0 2 * * ?");
        assertThat(details.getDescription()).isEqualTo("演示");
        assertThat(details.getGroup()).isEqualTo("snowdrift-job");
        assertThat(details.getStatus()).isEqualTo(JobStatusEnum.NORMAL);
        assertThat(details.getParams()).containsEntry("p", "v");
        assertThat(details.getLastFireTime()).isNotNull();
        assertThat(details.getNextFireTime()).isNull();
    }

    @Test
    @DisplayName("getJob：triggerStatus≠1 映射为 PAUSED")
    void getJobPausedStatus() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/pageList"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":{\"total\":1,\"data\":[{"
                        + "\"id\":42,\"jobGroup\":7,\"executorHandler\":\"h\",\"triggerStatus\":0}]}}");

        assertThat(service.getJob(XxlJobKey.newInstance(42, 7)).getStatus()).isEqualTo(JobStatusEnum.PAUSED);
    }

    @Test
    @DisplayName("exists：未命中返回 false")
    void existsFalseWhenNotFound() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/pageList"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":{\"total\":0,\"data\":[]}}");

        assertThat(service.exists(XxlJobKey.newInstance(999, 7))).isFalse();
    }

    @Test
    @DisplayName("listJobs：executorParam 非法 JSON 时降级为空 Map，不抛出")
    void listJobsInvalidExecutorParamDegrades() {
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/pageList"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":{\"total\":1,\"data\":[{"
                        + "\"id\":42,\"jobGroup\":7,\"executorHandler\":\"h\",\"executorParam\":\"not-json\"}]}}");

        List<JobDetails> list = service.listJobs();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getParams()).isEmpty();
    }

    @Test
    @DisplayName("listJobs：AppName 反查失败时降级为 jobGroup 数字 ID（回归 Guava 兜底缺陷）")
    void listJobsFallsBackToNumericGroupWhenAppNameUnresolved() {
        // jobGroup=99 不在默认分组页（仅含 id=7）中 → loadExecutorAppName 抛 ScheduleException
        // → 经 Guava 包装为 UncheckedExecutionException → 修复后应被捕获并降级为数字 ID
        mockedHttp.when(() -> HttpUtil.postForm(contains("/jobinfo/pageList"), any(), any(), any()))
                .thenReturn("{\"code\":200,\"data\":{\"total\":1,\"data\":[{"
                        + "\"id\":42,\"jobGroup\":99,\"executorHandler\":\"h\"}]}}");

        List<JobDetails> list = service.listJobs("snowdrift-job");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getGroup()).isEqualTo("99");
    }
}
