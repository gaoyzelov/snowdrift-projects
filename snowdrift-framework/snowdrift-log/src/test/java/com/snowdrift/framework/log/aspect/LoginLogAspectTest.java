package com.snowdrift.framework.log.aspect;

import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.http.HttpContext;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.annotation.LoginLog;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LoginLogAspectTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 登录日志切面单元测试
 * @since 1.0.0
 */
class LoginLogAspectTest {

    static final class CapturingLogService implements ILogService {
        final List<LoginLogHolder> loginLogs = new ArrayList<>();
        Throwable failure;

        @Override
        public void saveApiLog(ApiLogHolder holder) {
        }

        @Override
        public void saveLoginLog(LoginLogHolder holder) {
            if (failure != null) {
                throwFailure();
            }
            loginLogs.add(holder);
        }
        /** failure 声明为 Throwable 以便同时模拟 Exception 与 Error；用例只会放入非受检异常 */
        private void throwFailure() {
            if (failure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) failure;
        }


        @Override
        public void saveOperateLog(OperateLogHolder holder) {
        }

        LoginLogHolder single() {
            assertThat(loginLogs).hasSize(1);
            return loginLogs.get(0);
        }
    }

    /** 自引用对象：fastjson2 默认不做循环检测，序列化时递归失败（默认栈大小下抛 JSONException） */
    public static class CyclicDto {
        private CyclicDto self = this;

        public CyclicDto getSelf() {
            return self;
        }

        public void setSelf(CyclicDto self) {
            this.self = self;
        }
    }

    public static class LoginDTO {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @SuppressWarnings("unused")
    static class Annotated {

        @LoginLog
        public Result<String> login(LoginDTO dto) {
            return Result.ok("token");
        }

        @LoginLog(accountField = "account")
        public Result<String> loginByAccount(Map<String, Object> param) {
            return Result.ok("token");
        }

        @LoginLog(enable = false)
        public Result<String> disabled(LoginDTO dto) {
            return Result.ok("token");
        }
    }

    private CapturingLogService logService;
    private LoginLogAspect aspect;
    private ch.qos.logback.classic.Logger aspectLogger;
    private ch.qos.logback.classic.Level originalLevel;
    private boolean originalAdditive;
    private ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> errorAppender;

    @BeforeEach
    void setUp() {
        logService = new CapturingLogService();
        aspect = new LoginLogAspect(logService);
        // 部分用例刻意触发采集失败，切面会记 error；这里静音以保持构建输出干净
        aspectLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(LoginLogAspect.class);
        originalLevel = aspectLogger.getLevel();
        originalAdditive = aspectLogger.isAdditive();
        aspectLogger.setAdditive(false);
        aspectLogger.setLevel(ch.qos.logback.classic.Level.ERROR);
        errorAppender = new ch.qos.logback.core.read.ListAppender<>();
        errorAppender.start();
        aspectLogger.addAppender(errorAppender);
    }

    @AfterEach
    void clearContext() {
        HttpContextHolder.clear();
        aspectLogger.detachAppender(errorAppender);
        aspectLogger.setLevel(originalLevel);
        aspectLogger.setAdditive(originalAdditive);
    }

    /** 切面在采集失败时会记 ERROR；这里断言“常规场景不产生噪音” */
    private List<String> errorMessages() {
        return errorAppender.list.stream()
                .filter(event -> event.getLevel() == ch.qos.logback.classic.Level.ERROR)
                .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static LoginLog anno(String method, Class<?>... parameterTypes) throws Exception {
        Method target = Annotated.class.getMethod(method, parameterTypes);
        return target.getAnnotation(LoginLog.class);
    }

    private static JoinPoint joinPoint(Object... args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private static LoginDTO dto() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("P@ssw0rd!");
        return dto;
    }

    @Test
    @DisplayName("登录成功：status=OK、消息固定为登录成功、账号从入参提取")
    void success() throws Exception {
        HttpContextHolder.setContext(HttpContext.builder()
                .ip("1.2.3.4").ipLocation("中国 北京").userAgent("ua").build());

        aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.ok("token"));

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.OK.code());
        assertThat(holder.getMsg()).isEqualTo("登录成功");
        assertThat(holder.getUsername()).isEqualTo("admin");
        assertThat(holder.getIp()).isEqualTo("1.2.3.4");
        assertThat(holder.getIpLocation()).isEqualTo("中国 北京");
        assertThat(holder.getUa()).isEqualTo("ua");
        assertThat(holder.getLoginTime()).isNotNull();
    }

    @Test
    @DisplayName("无 HTTP 上下文时 ip/ua 为空，不影响记录")
    void successWithoutHttpContext() throws Exception {
        aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.ok());

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.OK.code());
        assertThat(holder.getIp()).isNull();
        assertThat(holder.getIpLocation()).isNull();
        assertThat(holder.getUa()).isNull();
        assertThat(holder.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("业务失败（Result.code != OK）：status=ERR，消息取业务消息")
    void businessFailure() throws Exception {
        aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.err(ResultCode.BAD_REQUEST));

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo(ResultCode.BAD_REQUEST.msg());
    }

    @Test
    @DisplayName("业务失败且消息为空时回落到 ERR 内置文案")
    void businessFailureWithoutMessage() throws Exception {
        aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.err((String) null));

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo(ResultCode.ERR.msg());
    }

    @Test
    @DisplayName("Result.code 为 null（new Result<>() 未赋值）时不得抛 NPE，且按失败记录（回归：此前整条日志被吞掉）")
    void nullResultCodeIsRecordedAsFailure() throws Exception {
        Result<String> withoutCode = new Result<>();

        aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), withoutCode);

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo(ResultCode.ERR.msg());
        assertThat(holder.getUsername()).isEqualTo("admin");
        assertThat(errorMessages()).isEmpty();
    }

    @Test
    @DisplayName("抛异常：status=ERR，消息取异常消息")
    void throwing() throws Exception {
        aspect.afterThrowing(joinPoint(dto()), anno("login", LoginDTO.class),
                new IllegalStateException("密码错误"));

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo("密码错误");
        assertThat(holder.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("抛异常且消息为 null 时回落到异常类名")
    void throwingWithoutMessage() throws Exception {
        aspect.afterThrowing(joinPoint(dto()), anno("login", LoginDTO.class), new IllegalStateException());

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("自定义 accountField：从 Map 入参中提取账号")
    void customAccountFieldFromMap() throws Exception {
        aspect.afterReturning(joinPoint(Map.of("account", "root", "secret", "x")),
                anno("loginByAccount", Map.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("root");
    }

    @Test
    @DisplayName("提取不到账号时回落为 unknown")
    void unknownUsername() throws Exception {
        LoginDTO withoutUsername = new LoginDTO();

        aspect.afterReturning(joinPoint(withoutUsername), anno("login", LoginDTO.class), Result.ok());
        assertThat(logService.single().getUsername()).isEqualTo("unknown");

        logService.loginLogs.clear();
        aspect.afterReturning(joinPoint(), anno("login", LoginDTO.class), Result.ok());
        assertThat(logService.single().getUsername()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("遇到无法转换的入参（如 String）不中断，继续从后续入参提取")
    void toleratesUnconvertibleArgs() throws Exception {
        aspect.afterReturning(joinPoint("plain-string", dto()), anno("login", LoginDTO.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("自引用入参不得影响登录流程，日志照记")
    void cyclicDtoDoesNotBreakLogin() {
        CyclicDto cyclic = new CyclicDto();

        assertThatCode(() -> aspect.afterReturning(joinPoint(cyclic), anno("login", LoginDTO.class), Result.ok()))
                .doesNotThrowAnyException();

        assertThat(logService.single().getStatus()).isEqualTo(ResultCode.OK.code());
        assertThat(logService.single().getUsername()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("enable=false 时不记录")
    void disabled() throws Exception {
        aspect.afterReturning(joinPoint(dto()), anno("disabled", LoginDTO.class), Result.ok());

        assertThat(logService.loginLogs).isEmpty();
    }

    @Test
    @DisplayName("裸参数登录（@RequestParam String username）按形参名解析出账号，且不再刷 ERROR")
    void resolvesAccountByParameterName() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"root", "P@ssw0rd!"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"username", "password"});

        aspect.afterReturning(joinPoint, anno("login", LoginDTO.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("root");
        assertThat(errorMessages()).isEmpty();
    }

    @Test
    @DisplayName("形参名与 accountField 不匹配时回落 unknown，且不产生 ERROR 噪音")
    void scalarWithoutMatchingParameterName() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"root"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"account"});

        aspect.afterReturning(joinPoint, anno("login", LoginDTO.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("unknown");
        assertThat(errorMessages()).isEmpty();
    }

    @Test
    @DisplayName("无法取到形参名（未开启 -parameters 等）时不影响日志记录")
    void parameterNamesUnavailable() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"root"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenThrow(new UnsupportedOperationException("无参数名"));

        aspect.afterReturning(joinPoint, anno("login", LoginDTO.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("unknown");
        assertThat(logService.loginLogs).hasSize(1);
    }

    @Test
    @DisplayName("对象入参优先于形参名：DTO 中已有账号时不再看形参")
    void dtoTakesPrecedenceOverParameterName() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto()});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"username"});

        aspect.afterReturning(joinPoint, anno("login", LoginDTO.class), Result.ok());

        assertThat(logService.single().getUsername()).isEqualTo("admin");
        assertThat(errorMessages()).isEmpty();
    }

    @Test
    @DisplayName("日志服务抛异常不影响登录流程")
    void logServiceFailureDoesNotBreakLogin() throws Exception {
        logService.failure = new IllegalStateException("存储不可用");

        assertThatCode(() -> aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.ok()))
                .doesNotThrowAnyException();
        assertThatCode(() -> aspect.afterThrowing(joinPoint(dto()), anno("login", LoginDTO.class),
                new IllegalStateException("密码错误"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("日志服务抛 Error 同样不影响登录流程（兜底范围是 Throwable）")
    void errorFromLogServiceDoesNotBreakLogin() {
        logService.failure = new StackOverflowError("模拟日志链路溢出");

        assertThatCode(() -> aspect.afterReturning(joinPoint(dto()), anno("login", LoginDTO.class), Result.ok()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("登录方法抛 Error 时也被记录（@AfterThrowing 绑定 Throwable）")
    void errorIsRecordedByAfterThrowing() throws Exception {
        aspect.afterThrowing(joinPoint(dto()), anno("login", LoginDTO.class), new StackOverflowError("栈溢出"));

        LoginLogHolder holder = logService.single();
        assertThat(holder.getStatus()).isEqualTo(ResultCode.ERR.code());
        assertThat(holder.getMsg()).isEqualTo("栈溢出");
    }
}
