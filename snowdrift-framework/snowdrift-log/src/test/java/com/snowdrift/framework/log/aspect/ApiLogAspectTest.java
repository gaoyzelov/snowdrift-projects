package com.snowdrift.framework.log.aspect;

import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.annotation.ApiLog;
import com.snowdrift.framework.log.enums.BizTypeEnum;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ApiLogAspectTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 接口访问日志切面单元测试
 * @since 1.0.0
 */
class ApiLogAspectTest {

    /** 采集器：把切面产出的 Holder 收集起来断言 */
    static final class CapturingLogService implements ILogService {
        final List<ApiLogHolder> apiLogs = new ArrayList<>();
        Throwable failure;

        @Override
        public void saveApiLog(ApiLogHolder holder) {
            if (failure != null) {
                throwFailure();
            }
            apiLogs.add(holder);
        }
        /** failure 声明为 Throwable 以便同时模拟 Exception 与 Error；用例只会放入非受检异常 */
        private void throwFailure() {
            if (failure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) failure;
        }


        @Override
        public void saveLoginLog(LoginLogHolder holder) {
        }

        @Override
        public void saveOperateLog(OperateLogHolder holder) {
        }

        ApiLogHolder single() {
            assertThat(apiLogs).hasSize(1);
            return apiLogs.get(0);
        }
    }

    /** 用于取注解实例与参数形态 */
    @SuppressWarnings("unused")
    static class Annotated {

        @ApiLog(bizType = BizTypeEnum.INSERT, module = "订单管理", summary = "创建订单")
        public Result<String> defaultOptions(String arg) {
            return Result.ok("ok");
        }

        @ApiLog(bizType = BizTypeEnum.UPDATE, module = "用户", summary = "修改用户",
                saveParams = true, saveResult = true, mask = {"password", "ID", "phone"})
        public Result<String> saveAll(String arg) {
            return Result.ok("ok");
        }

        @ApiLog(bizType = BizTypeEnum.SELECT, enable = false)
        public Result<String> disabled(String arg) {
            return Result.ok("ok");
        }

        /** 未配置 mask，用于验证内置凭据字段的默认脱敏 */
        @ApiLog(bizType = BizTypeEnum.SELECT, module = "查询", summary = "未配置掩码",
                saveParams = true, saveResult = true)
        public Result<String> noMask(String arg) {
            return Result.ok("ok");
        }
    }

    /** 含敏感字段的入参/出参 */
    public static class Payload {
        private Long id = 1001L;
        private String password = "P@ssw0rd!";
        private String phone = "13812345678";
        private Map<String, Object> nested = new LinkedHashMap<>();
        private List<Object> list = new ArrayList<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Map<String, Object> getNested() {
            return nested;
        }

        public void setNested(Map<String, Object> nested) {
            this.nested = nested;
        }

        public List<Object> getList() {
            return list;
        }

        public void setList(List<Object> list) {
            this.list = list;
        }
    }

    private CapturingLogService logService;
    private ApiLogAspect aspect;

    @BeforeEach
    void setUp() {
        logService = new CapturingLogService();
        aspect = new ApiLogAspect(logService);
        ReflectionTestUtils.setField(aspect, "appName", "test-app");
    }

    @AfterEach
    void clearContexts() {
        HttpContextHolder.clear();
        SecurityContextHolder.clear();
    }

    private static ApiLog anno(String method, Class<?>... parameterTypes) throws Exception {
        Method target = Annotated.class.getMethod(method, parameterTypes);
        return target.getAnnotation(ApiLog.class);
    }

    private static ProceedingJoinPoint joinPoint(Object result, Object... args) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private static ProceedingJoinPoint failingJoinPoint(Throwable error, Object... args) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(error);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    /** 自引用对象：fastjson2 默认不做循环检测，序列化时递归失败（默认栈大小下抛 JSONException，栈更小时可能是 StackOverflowError） */
    public static class Cyclic {
        private Cyclic self = this;

        public Cyclic getSelf() {
            return self;
        }

        public void setSelf(Cyclic self) {
            this.self = self;
        }
    }

    private static void setupContexts() {
        setupContexts(Map.of("page", "1"));
    }

    private static void setupContexts(Map<String, String> paramMap) {
        HttpContextHolder.setContext(HttpContext.builder()
                .uri("/api/order").method("POST").ip("1.2.3.4").userAgent("ua")
                .paramMap(paramMap)
                .build());
        SecurityContextHolder.setContext(SecurityContext.builder()
                .userId(1L).username("admin").nickname("管理员").tenantId(100L)
                .build());
    }

    @Test
    @DisplayName("默认注解（不记参数/结果）只记录元信息")
    void defaultOptionsRecordMetadataOnly() throws Throwable {
        setupContexts();

        aspect.around(joinPoint(Result.ok("ok"), "arg"), anno("defaultOptions", String.class));

        ApiLogHolder holder = logService.single();
        assertThat(holder.getAppName()).isEqualTo("test-app");
        assertThat(holder.getBizModule()).isEqualTo("订单管理");
        assertThat(holder.getSummary()).isEqualTo("创建订单");
        assertThat(holder.getBizType()).isEqualTo(BizTypeEnum.INSERT.getCode());
        assertThat(holder.getStatus()).isEqualTo(ResultCode.OK.code());

        assertThat(holder.getMethod()).isEqualTo("POST");
        assertThat(holder.getUri()).isEqualTo("/api/order");
        assertThat(holder.getIp()).isEqualTo("1.2.3.4");
        assertThat(holder.getUa()).isEqualTo("ua");
        assertThat(holder.getUserId()).isEqualTo(1L);
        assertThat(holder.getTenantId()).isEqualTo(100L);
        assertThat(holder.getOperator()).isEqualTo("管理员");
        assertThat(holder.getOperateTime()).isNotNull();
        assertThat(holder.getDuration()).isNotNegative();

        // 默认不记录请求参数与响应结果（防止口令等默认落库）
        assertThat(holder.getRequestParams()).isEmpty();
        assertThat(holder.getResponseBody()).isEmpty();
    }

    @Test
    @DisplayName("无 Http/Security 上下文时字段为空且不抛异常（如定时任务、内部调用）")
    void worksWithoutContexts() throws Throwable {
        aspect.around(joinPoint(Result.ok(), "arg"), anno("defaultOptions", String.class));

        ApiLogHolder holder = logService.single();
        assertThat(holder.getUri()).isNull();
        assertThat(holder.getIp()).isNull();
        assertThat(holder.getUserId()).isNull();
        assertThat(holder.getOperator()).isNull();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.OK.code());
    }

    @Test
    @DisplayName("业务返回失败（Result.code != OK）记为失败并带业务消息")
    void businessFailure() throws Throwable {
        aspect.around(joinPoint(Result.err(ResultCode.BAD_REQUEST)), anno("defaultOptions", String.class));

        ApiLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getErrorMsg()).isEqualTo(ResultCode.BAD_REQUEST.msg());
    }

    @Test
    @DisplayName("方法抛异常：记为失败、记录根因消息，并原样抛出")
    void exceptionIsRecordedAndRethrown() throws Throwable {
        IllegalStateException root = new IllegalStateException("库存不足");
        Throwable wrapper = new RuntimeException("下单失败", root);

        assertThatThrownBy(() -> aspect.around(failingJoinPoint(wrapper), anno("defaultOptions", String.class)))
                .isSameAs(wrapper);

        ApiLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        // 记录的是 ExceptionUtils.getRootCauseMessage 的完整形态（异常类名 + 消息），不是裸消息
        assertThat(holder.getErrorMsg()).isEqualTo("IllegalStateException: 库存不足");
    }

    @Test
    @DisplayName("抛出 Error 时同样记为失败，并原样抛出")
    void errorIsRecordedAndRethrown() throws Throwable {
        AssertionError error = new AssertionError("boom");

        assertThatThrownBy(() -> aspect.around(failingJoinPoint(error), anno("defaultOptions", String.class)))
                .isSameAs(error);

        assertThat(logService.single().getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(logService.single().getErrorMsg()).contains("boom");
    }

    @Test
    @DisplayName("注解 enable=false 时完全不记录")
    void disabledAnnotationRecordsNothing() throws Throwable {
        aspect.around(joinPoint(Result.ok()), anno("disabled", String.class));

        assertThat(logService.apiLogs).isEmpty();
    }

    @Test
    @DisplayName("非 Result 返回值按成功记录")
    void nonResultReturnValueIsSuccess() throws Throwable {
        aspect.around(joinPoint("plain-string"), anno("defaultOptions", String.class));

        assertThat(logService.single().getStatus()).isEqualTo(ResultCode.OK.code());
        assertThat(logService.single().getErrorMsg()).isNull();
    }

    @Test
    @DisplayName("saveParams=true：记录入参 JSON，敏感字段被脱敏")
    void savesMaskedParams() throws Throwable {
        Payload payload = new Payload();

        aspect.around(joinPoint(Result.ok(), payload), anno("saveAll", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams).contains("\"id\"", "******");
        assertThat(requestParams).doesNotContain("P@ssw0rd!", "13812345678");
    }

    @Test
    @DisplayName("mask 递归生效：嵌套对象与数组内的同名字段一并脱敏（大小写不敏感）")
    void maskIsRecursiveAndCaseInsensitive() throws Throwable {
        Payload payload = new Payload();
        payload.getNested().put("PASSWORD", "nested-secret");
        payload.getNested().put("other", "keep-me");
        payload.getList().add(Map.of("phone", "13900000000"));

        aspect.around(joinPoint(Result.ok(), payload), anno("saveAll", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams).contains("keep-me", "******");
        assertThat(requestParams).doesNotContain("nested-secret", "13900000000");
    }

    @Test
    @DisplayName("saveResult=true：响应体同样按 mask 规则脱敏")
    void masksResponseBody() throws Throwable {
        Payload response = new Payload();

        aspect.around(joinPoint(Result.ok(response)), anno("saveAll", String.class));

        String responseBody = logService.single().getResponseBody();
        assertThat(responseBody).contains("******");
        assertThat(responseBody).doesNotContain("P@ssw0rd!", "13812345678");
    }

    @Test
    @DisplayName("深度超过上限的嵌套内容整体置为掩码，不会漏出原文")
    void deepNestingIsMasked() throws Throwable {
        Map<String, Object> deep = new LinkedHashMap<>();
        Map<String, Object> cursor = deep;
        for (int i = 0; i < 30; i++) {
            Map<String, Object> child = new LinkedHashMap<>();
            cursor.put("level" + i, child);
            cursor = child;
        }
        cursor.put("password", "deep-secret");

        aspect.around(joinPoint(Result.ok(), deep), anno("saveAll", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams).isNotBlank().doesNotContain("deep-secret");
    }

    @Test
    @DisplayName("URL 参数（paramMap）与入参一起记录")
    void mergesUrlParams() throws Throwable {
        setupContexts();

        aspect.around(joinPoint(Result.ok(), new Payload()), anno("saveAll", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams).contains("\"params\"", "\"page\"", "\"args\"");
    }

    @Test
    @DisplayName("Servlet/流/文件参数被剔除；全部被剔除时不记录参数")
    void filtersUnserializableArgs() throws Throwable {
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2});
        MockHttpServletRequest request = new MockHttpServletRequest();

        aspect.around(joinPoint(Result.ok(), request, stream), anno("saveAll", String.class));
        assertThat(logService.single().getRequestParams()).isEmpty();

        logService.apiLogs.clear();
        MockMultipartFile[] files = {new MockMultipartFile("f", "a.bin", "application/octet-stream", new byte[16])};
        aspect.around(joinPoint(Result.ok(), (Object) files), anno("saveAll", String.class));
        // 数组形式的文件参数不会被 filterArgs 剔除，但序列化会安全失败 → 不落库（也不会泄漏文件内容）
        assertThat(logService.single().getRequestParams()).isEmpty();
    }

    @Test
    @DisplayName("入参不可序列化时，URL 参数仍被保留（坏参数不再拖垮整段）")
    void keepsUrlParamsWhenArgCannotBeSerialized() throws Throwable {
        setupContexts();
        MockMultipartFile[] files = {new MockMultipartFile("f", "a.bin", "application/octet-stream", new byte[16])};

        aspect.around(joinPoint(Result.ok(), (Object) files), anno("saveAll", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams).isNotBlank().contains("page", "1");
    }

    // ========== 内置凭据字段的默认脱敏（@ApiLog 未配置 mask 时也生效） ==========

    @Test
    @DisplayName("未配置 mask 时，URL 参数里的凭据字段默认脱敏，不得落库明文")
    void masksCredentialUrlParamsWithoutAnnotationMask() throws Throwable {
        setupContexts(Map.of(
                "page", "1",
                "password", "P@ssw0rd!",
                "refreshToken", "rt.abc.def"));

        aspect.around(joinPoint(Result.ok(), new Payload()), anno("noMask", String.class));

        String requestParams = logService.single().getRequestParams();
        assertThat(requestParams)
                .doesNotContain("P@ssw0rd!", "rt.abc.def")
                .contains("page", "1");
    }

    @Test
    @DisplayName("凭据词按后缀匹配：userPassword/clientSecret 等组合命名同样脱敏")
    void masksCredentialSuffixedKeys() throws Throwable {
        setupContexts(Map.of(
                "userPassword", "u-secret",
                "clientSecret", "c-secret",
                "csrfToken", "t-secret"));

        aspect.around(joinPoint(Result.ok(), new Payload()), anno("noMask", String.class));

        assertThat(logService.single().getRequestParams())
                .doesNotContain("u-secret", "c-secret", "t-secret");
    }

    @Test
    @DisplayName("响应体里的凭据字段同样默认脱敏（未配置 mask 时）")
    void masksCredentialResponseBody() throws Throwable {
        setupContexts();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", "at.xyz");
        body.put("nickname", "管理员");

        aspect.around(joinPoint(Result.ok(body), new Payload()), anno("noMask", String.class));

        String responseBody = logService.single().getResponseBody();
        assertThat(responseBody).doesNotContain("at.xyz").contains("管理员");
    }

    @Test
    @DisplayName("普通字段不受影响：不因默认脱敏把常规入参一并抹掉")
    void ordinaryFieldsSurviveDefaultMasking() throws Throwable {
        setupContexts(Map.of("page", "1", "keyword", "tokenizer"));

        aspect.around(joinPoint(Result.ok(), new Payload()), anno("noMask", String.class));

        assertThat(logService.single().getRequestParams())
                .contains("page", "1", "keyword", "tokenizer");
    }

    @Test
    @DisplayName("自引用入参（fastjson2 在默认栈大小下抛 JSONException）不得中断业务，日志照常记录")
    void unserializableGraphDoesNotBreakBusiness() throws Throwable {
        Cyclic cyclic = new Cyclic();
        Result<String> expected = Result.ok("ok");

        Object result = aspect.around(joinPoint(expected, cyclic), anno("saveAll", String.class));

        assertThat(result).isSameAs(expected);
        assertThat(logService.apiLogs).hasSize(1);
        assertThat(logService.single().getStatus()).isEqualTo(ResultCode.OK.code());
    }

    @Test
    @DisplayName("日志服务自身抛异常不影响业务返回（切面在 finally 中兜底）")
    void logServiceFailureDoesNotBreakBusiness() throws Throwable {
        logService.failure = new IllegalStateException("存储不可用");

        Object result = aspect.around(joinPoint(Result.ok("ok"), "arg"), anno("defaultOptions", String.class));

        assertThat(result).isInstanceOf(Result.class);
        assertThat(((Result<?>) result).getCode()).isEqualTo(ResultCode.OK.code());
    }

    @Test
    @DisplayName("日志链路抛 Error（如 StackOverflowError）同样不影响业务返回：兜底范围是 Throwable")
    void errorFromLogServiceDoesNotBreakBusiness() throws Throwable {
        logService.failure = new StackOverflowError("模拟序列化递归溢出");
        Result<String> expected = Result.ok("ok");

        Object result = aspect.around(joinPoint(expected, "arg"), anno("defaultOptions", String.class));

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("业务方法抛 Error 时：仍然记失败、原样抛出，且日志链路的异常不得掩盖它")
    void businessErrorIsPreservedEvenIfLoggingFails() throws Throwable {
        logService.failure = new StackOverflowError("模拟日志链路溢出");
        AssertionError businessError = new AssertionError("业务断言失败");

        assertThatThrownBy(() -> aspect.around(failingJoinPoint(businessError), anno("defaultOptions", String.class)))
                .isSameAs(businessError);
    }
}
