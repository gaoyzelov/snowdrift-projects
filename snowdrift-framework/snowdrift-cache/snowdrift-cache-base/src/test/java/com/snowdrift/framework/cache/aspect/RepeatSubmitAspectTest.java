package com.snowdrift.framework.cache.aspect;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.cache.annotation.RepeatSubmit;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RepeatSubmitAspectTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 防重复提交切面单元测试（key 形态：repeat:submit:ip:&lt;ip&gt;:&lt;uri&gt;:&lt;SpEL 结果或参数指纹&gt;）
 * @since 1.0.0
 */
class RepeatSubmitAspectTest {

    private static final String IP = "127.0.0.1";

    private static final String URI = "/order/create";

    /** 新的 key 前缀：身份（IP）+ 接口（URI）+ 作用域 */
    private static final String KEY_PREFIX = "repeat:submit:ip:" + IP + ":" + URI + ":";

    /** 可观察的内存缓存实现：记录 putIfAbsent 的 TTL，可模拟删除失败 */
    static class RecordingCache implements ICacheService {
        final Map<String, String> store = new HashMap<>();
        Duration lastTtl;
        boolean failOnDelete;

        @Override
        public boolean putIfAbsent(String key, Object value, Duration ttl) {
            lastTtl = ttl;
            return store.putIfAbsent(key, (String) value) == null;
        }

        @Override
        public <T> T get(String key, Class<T> type) {
            String value = store.get(key);
            return value == null ? null : type.cast(value);
        }

        @Override
        public boolean delete(String key) {
            if (failOnDelete) {
                throw new IllegalStateException("删除失败");
            }
            return store.remove(key) != null;
        }

        @Override
        public <T> T hget(String key, String hashKey, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(String key, java.lang.reflect.Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T hget(String key, String hashKey, java.lang.reflect.Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, Object value, Duration ttl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void hput(String key, String hashKey, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putIfAbsent(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void hdelete(String key, String hashKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long batchDelete(Collection<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean expire(String key, Duration ttl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getExpire(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long increment(String key, Duration ttl) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unused")
    static class Annotated {

        @RepeatSubmit(key = "'order:' + #orderNo", interval = 30)
        public String create(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "#orderNo")
        public String keyFromParam(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = " ")
        public String blankKey(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "static:fixed")
        public String fixedKey(String orderNo) {
            return "created";
        }

        @RepeatSubmit(key = "#orderNo", interval = 0)
        public String zeroInterval(String orderNo) {
            return "created";
        }

        /** SpEL 参数名不存在 → SpELUtil 返回 null（不抛异常） */
        @RepeatSubmit(key = "#notExists")
        public String badSpel(String orderNo) {
            return "created";
        }

        /** 未指定 key：走「参数指纹」默认分支 */
        @RepeatSubmit
        public String submit(String body) {
            return "created";
        }

        /** 未指定 key + 请求绑定类参数（不应参与指纹） */
        @RepeatSubmit
        public String submitWithInfraArgs(String body, HttpServletRequest request, Errors errors) {
            return "created";
        }

        /** 未指定 key + 文件参数（按 文件名 + 大小 参与指纹） */
        @RepeatSubmit
        public String upload(MockMultipartFile file) {
            return "created";
        }

        /** 未指定 key + 无 getter 的 POJO（默认按 getter 序列化会得到 {}） */
        @RepeatSubmit
        public String submitDto(NoGetterDto dto) {
            return "created";
        }

        /** 未指定 key + Map 参数（插入顺序不应影响指纹） */
        @RepeatSubmit
        public String submitMap(Map<String, Object> body) {
            return "created";
        }
    }

    /** 私有字段 + 无 getter：用于验证参数指纹必须按字段序列化 */
    static class NoGetterDto {
        private String orderNo;
        private int amount;

        NoGetterDto(String orderNo, int amount) {
            this.orderNo = orderNo;
            this.amount = amount;
        }
    }

    private RecordingCache cache;
    private RepeatSubmitAspect aspect;

    @BeforeEach
    void setUp() {
        cache = new RecordingCache();
        aspect = new RepeatSubmitAspect(cache);

        // 新版切面以当前请求为 key 依据，测试需模拟 Web 上下文
        MockHttpServletRequest request = new MockHttpServletRequest("POST", URI);
        request.setRemoteAddr(IP);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static Method method(String name, Class<?>... types) throws Exception {
        return Annotated.class.getMethod(name, types);
    }

    private static RepeatSubmit anno(Method method) {
        return method.getAnnotation(RepeatSubmit.class);
    }

    /** 构造带真实方法签名的切点：SpEL 解析依赖 MethodSignature#getMethod() */
    private static ProceedingJoinPoint joinPoint(Method method, Object[] args) {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    /** 业务正常返回的切点 */
    private static ProceedingJoinPoint succeeding(Method method, Object[] args, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(method, args);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    /** 业务抛异常的切点 */
    private static ProceedingJoinPoint failing(Method method, Object[] args, Throwable error) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(method, args);
        when(joinPoint.proceed()).thenThrow(error);
        return joinPoint;
    }

    private static Object[] args(String... values) {
        return values;
    }

    @Test
    @DisplayName("安全边界：IPv6 客户端地址中的冒号必须转义，否则身份段破坏 key 的段边界")
    void ipv6AddressIsEscapedInKey() throws Throwable {
        // IPv6 规范化串本身含 ':'，裸拼会让「身份段」与「接口段」的边界不可分辨。
        // 注意 IpUtil 会先把地址规范化为完整 8 组形式（2001:db8::1 -> 2001:db8:0:0:0:0:0:1）。
        MockHttpServletRequest request = new MockHttpServletRequest("POST", URI);
        request.setRemoteAddr("2001:db8::1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = method("create", String.class);
        aspect.around(succeeding(method, args("A"), "created"), anno(method));

        assertThat(cache.store.keySet()).containsExactly(
                "repeat:submit:ip:2001\\:db8\\:0\\:0\\:0\\:0\\:0\\:1:/order/create:order\\:A");
    }

    @Test
    @DisplayName("首次提交：写入标记并执行业务，TTL 取注解 interval（key = 前缀 + 身份 + 接口 + SpEL 结果）")
    void firstSubmitProceeds() throws Throwable {
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A001"), "ok");

        Object result = aspect.around(joinPoint, anno(method));

        assertThat(result).isEqualTo("ok");
        assertThat(cache.store).containsKey(KEY_PREFIX + "order\\:A001");
        assertThat(cache.lastTtl).isEqualTo(Duration.of(30, ChronoUnit.SECONDS));
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("时间窗口内重复提交：抛 BizException 且不执行业务")
    void duplicateSubmitRejected() throws Throwable {
        cache.store.put(KEY_PREFIX + "order\\:A001", "previous-nonce");
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A001"), "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(BizException.class)
                .hasMessage("请勿重复提交，请稍后重试");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("key 为 #参数 形式时用参数值作为作用域")
    void keyFromSingleParameter() throws Throwable {
        Method method = method("keyFromParam", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A002"), "ok");

        assertThat(aspect.around(joinPoint, anno(method))).isEqualTo("ok");
        assertThat(cache.store).containsKey(KEY_PREFIX + "A002");
    }

    @Test
    @DisplayName("key 不含 # 时按静态值作为作用域（分隔符按 SnowdriftKeyGenerator 规则转义）")
    void staticKey() throws Throwable {
        Method method = method("fixedKey", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A002"), "ok");

        assertThat(aspect.around(joinPoint, anno(method))).isEqualTo("ok");
        assertThat(cache.store).containsKey(KEY_PREFIX + "static\\:fixed");
    }

    @Test
    @DisplayName("业务抛异常：删除本请求写入的标记，允许修正后重新提交，原异常照常抛出")
    void businessFailureClearsMarker() throws Throwable {
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = failing(method, args("A001"), new IllegalStateException("业务失败"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(IllegalStateException.class).hasMessage("业务失败");

        assertThat(cache.store).doesNotContainKey(KEY_PREFIX + "order\\:A001");
    }

    @Test
    @DisplayName("缓存中的标记已不是本请求写入的 nonce（已过期并被并发请求重写）：不删除")
    void doesNotDeleteForeignMarker() throws Throwable {
        String key = KEY_PREFIX + "order\\:A001";
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, args("A001"));
        AtomicReference<String> rewritten = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            // 模拟执行期间本请求标记过期、被其它请求写入新 nonce
            rewritten.set("other-nonce");
            cache.store.put(key, "other-nonce");
            throw new IllegalStateException("业务失败");
        });

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(cache.store).containsEntry(key, rewritten.get());
    }

    @Test
    @DisplayName("标记删除失败：仅记日志，不覆盖原始业务异常")
    void deleteFailureDoesNotMaskBusinessException() throws Throwable {
        cache.failOnDelete = true;
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = failing(method, args("A001"), new IllegalArgumentException("原始异常"));

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("原始异常");
    }

    @Test
    @DisplayName("业务抛中断异常：恢复中断标记后原样抛出")
    void interruptedRestoresFlag() throws Throwable {
        AtomicBoolean flagRestored = new AtomicBoolean();
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = failing(method, args("A001"), new InterruptedException("被中断"));

        try {
            aspect.around(joinPoint, anno(method));
        } catch (InterruptedException e) {
            flagRestored.set(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertThat(flagRestored).isTrue();
    }

    @Test
    @DisplayName("回归：interval=0 不再静默放行，直接抛 BizException（此前防重窗口为空、重复请求全部通过）")
    void zeroIntervalIsRejected() throws Throwable {
        Method method = method("zeroInterval", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A001"), "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(BizException.class)
                .hasMessage("防重间隔必须大于 0");
        verify(joinPoint, never()).proceed();
        assertThat(cache.store).isEmpty();
    }

    @Test
    @DisplayName("非 Web 环境（无请求上下文）直接放行：不写标记、不拦截")
    void nonWebContextPassesThrough() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A001"), "ok");

        assertThat(aspect.around(joinPoint, anno(method))).isEqualTo("ok");
        assertThat(cache.store).isEmpty();
    }

    @Test
    @DisplayName("key 为空白视为未指定：回落参数指纹分支，同参数重复提交仍被拒")
    void blankKeyFallsBackToFingerprint() throws Throwable {
        Method method = method("blankKey", String.class);
        ProceedingJoinPoint first = succeeding(method, args("A001"), "ok");

        assertThat(aspect.around(first, anno(method))).isEqualTo("ok");
        assertThat(cache.store).hasSize(1);
        assertThat(cache.store.keySet().iterator().next()).matches(KEY_PREFIX + "[0-9a-f]{32}");

        // 同一参数第二次提交 → 命中同一指纹
        ProceedingJoinPoint second = succeeding(method, args("A001"), "ok");
        assertThatThrownBy(() -> aspect.around(second, anno(method)))
                .isInstanceOf(BizException.class);
        verify(second, never()).proceed();
    }

    @Test
    @DisplayName("未指定 key：默认 key 为「身份 + 接口 + 参数指纹」，参数不同互不影响")
    void defaultKeyUsesParamsFingerprint() throws Throwable {
        Method method = method("submit", String.class);

        assertThat(aspect.around(succeeding(method, args("body-1"), "ok"), anno(method))).isEqualTo("ok");
        assertThat(cache.store).hasSize(1);

        // 同参数 → 拒绝
        assertThatThrownBy(() -> aspect.around(succeeding(method, args("body-1"), "ok"), anno(method)))
                .isInstanceOf(BizException.class);

        // 不同参数 → 不同指纹，放行
        assertThat(aspect.around(succeeding(method, args("body-2"), "ok"), anno(method))).isEqualTo("ok");
        assertThat(cache.store).hasSize(2);
    }

    @Test
    @DisplayName("回归：SpEL 求值为 null 时跳过防重校验，不得把字面量 \"null\" 当成作用域")
    void spElResolutionEmptySkipsCheck() throws Throwable {
        Method method = method("badSpel", String.class);
        ProceedingJoinPoint first = succeeding(method, args("A001"), "ok");

        assertThat(aspect.around(first, anno(method))).isEqualTo("ok");
        // 若把 null 拼进 key，这里会出现 ...:null 并让同身份同接口的后续请求全部被误判为重复
        assertThat(cache.store).isEmpty();

        ProceedingJoinPoint second = succeeding(method, args("A001"), "ok");
        assertThat(aspect.around(second, anno(method))).isEqualTo("ok");
    }

    @Test
    @DisplayName("请求绑定类参数（HttpServletRequest / Errors）不参与参数指纹：同 body 不同请求仍被判为重复")
    void requestBoundArgsExcludedFromFingerprint() throws Throwable {
        Method method = method("submitWithInfraArgs", String.class, HttpServletRequest.class, Errors.class);
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", URI);
        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", URI);
        firstRequest.setRemoteAddr(IP);
        secondRequest.setRemoteAddr(IP);

        assertThat(aspect.around(succeeding(method,
                new Object[]{"same-body", firstRequest, mock(Errors.class)}, "ok"), anno(method))).isEqualTo("ok");

        // 换一个 request / Errors 实例：若它们参与指纹，key 会不同从而被放行
        ProceedingJoinPoint second = succeeding(method,
                new Object[]{"same-body", secondRequest, mock(Errors.class)}, "ok");
        assertThatThrownBy(() -> aspect.around(second, anno(method)))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("文件参数按「文件名 + 大小」参与指纹：同名同大小视为重复，大小不同放行")
    void multipartFileFingerprintsNameAndSize() throws Throwable {
        Method method = method("upload", MockMultipartFile.class);

        assertThat(aspect.around(succeeding(method, new Object[]{
                new MockMultipartFile("file", "a.txt", "text/plain", "12345".getBytes())}, "ok"),
                anno(method))).isEqualTo("ok");

        // 同名同大小的另一个实例 → 同一指纹 → 拒绝（若直接序列化 MultipartFile，两次实例不同也会不同）
        ProceedingJoinPoint duplicate = succeeding(method, new Object[]{
                new MockMultipartFile("file", "a.txt", "text/plain", "12345".getBytes())}, "ok");
        assertThatThrownBy(() -> aspect.around(duplicate, anno(method)))
                .isInstanceOf(BizException.class);

        // 大小不同 → 不同指纹 → 放行
        assertThat(aspect.around(succeeding(method, new Object[]{
                new MockMultipartFile("file", "a.txt", "text/plain", "123456".getBytes())}, "ok"),
                anno(method))).isEqualTo("ok");
    }

    @Test
    @DisplayName("回归：interval 校验先于请求上下文判断，非 Web 环境同样立即失败（配置错误不被放行分支掩盖）")
    void zeroIntervalFailsFastEvenWithoutWebContext() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        Method method = method("zeroInterval", String.class);
        ProceedingJoinPoint joinPoint = succeeding(method, args("A001"), "ok");

        assertThatThrownBy(() -> aspect.around(joinPoint, anno(method)))
                .isInstanceOf(BizException.class)
                .hasMessage("防重间隔必须大于 0");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("客户端 IP 无法识别时放行：不得用 unknown 当身份把不同客户端归并到同一个键")
    void unknownClientIpSkipsCheck() throws Throwable {
        // XFF 链含无法解析的节点 → IpUtil.getIp 判定为 unknown
        MockHttpServletRequest request = new MockHttpServletRequest("POST", URI);
        request.setRemoteAddr(IP);
        request.addHeader("X-Forwarded-For", "not-an-ip");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = method("create", String.class);
        assertThat(aspect.around(succeeding(method, args("A001"), "ok"), anno(method))).isEqualTo("ok");
        assertThat(cache.store).isEmpty();

        // 第二次同样放行：若用 unknown 当身份，这里会命中上一次的标记而被误判为重复提交
        assertThat(aspect.around(succeeding(method, args("A001"), "ok"), anno(method))).isEqualTo("ok");
    }

    @Test
    @DisplayName("参数指纹按字段序列化：无 getter 的入参不会因序列化成 {} 而让不同请求互相误判")
    void noGetterArgsAreFingerprintedByField() throws Throwable {
        Method method = method("submitDto", NoGetterDto.class);

        assertThat(aspect.around(succeeding(method, new Object[]{new NoGetterDto("A001", 100)}, "ok"), anno(method)))
                .isEqualTo("ok");
        // 不同字段值 → 不同指纹 → 必须放行（若按 getter 序列化，两者都是 {}，这里会被误拒）
        assertThat(aspect.around(succeeding(method, new Object[]{new NoGetterDto("A002", 200)}, "ok"), anno(method)))
                .isEqualTo("ok");
        assertThat(cache.store).hasSize(2);
    }

    @Test
    @DisplayName("参数指纹与 Map 键序无关：同内容不同插入顺序仍判为重复提交")
    void mapArgOrderDoesNotChangeFingerprint() throws Throwable {
        Method method = method("submitMap", Map.class);
        Map<String, Object> first = new java.util.LinkedHashMap<>();
        first.put("orderNo", "A001");
        first.put("amount", 100);
        Map<String, Object> second = new java.util.LinkedHashMap<>();
        second.put("amount", 100);
        second.put("orderNo", "A001");

        assertThat(aspect.around(succeeding(method, new Object[]{first}, "ok"), anno(method))).isEqualTo("ok");

        ProceedingJoinPoint duplicate = succeeding(method, new Object[]{second}, "ok");
        assertThatThrownBy(() -> aspect.around(duplicate, anno(method)))
                .isInstanceOf(BizException.class);
        assertThat(cache.store).hasSize(1);
    }

    @Test
    @DisplayName("URI 与作用域中的分隔符被转义：/a:b + c 与 /a + b:c 不会撞成同一个键")
    void uriAndScopeDelimitersAreEscaped() throws Throwable {
        Method method = method("keyFromParam", String.class);

        // 未转义时两者都会拼成 "repeat:submit:ip:127.0.0.1:/a:b:c"，第二次会被误判为重复提交
        assertThat(runWithRequest(method, "/a:b", "c")).isEqualTo("ok");
        assertThat(runWithRequest(method, "/a", "b:c")).isEqualTo("ok");

        assertThat(cache.store).containsOnlyKeys(
                "repeat:submit:ip:" + IP + ":/a\\:b:c",
                "repeat:submit:ip:" + IP + ":/a:b\\:c");
    }

    /** 用指定 URI 与参数执行一次切面 */
    private Object runWithRequest(Method method, String uri, String arg) throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(IP);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return aspect.around(succeeding(method, args(arg), "ok"), anno(method));
    }

    @Test
    @DisplayName("缓存不可用时异常直接向上抛出（基础设施故障调用方需感知）")
    void cacheFailurePropagates() throws Exception {
        ICacheService failingCache = mock(ICacheService.class);
        when(failingCache.putIfAbsent(anyString(), any(), any()))
                .thenThrow(new IllegalStateException("缓存不可用"));
        RepeatSubmitAspect failingAspect = new RepeatSubmitAspect(failingCache);
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, args("A001"));

        assertThatThrownBy(() -> failingAspect.around(joinPoint, anno(method)))
                .isInstanceOf(IllegalStateException.class).hasMessage("缓存不可用");
    }

    @Test
    @DisplayName("缺少缓存后端：即使非 Web 调用也 fail-closed，不执行业务")
    void missingCacheBackendFailsClosed() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        RepeatSubmitAspect missingBackendAspect = new RepeatSubmitAspect(null);
        Method method = method("create", String.class);
        ProceedingJoinPoint joinPoint = joinPoint(method, args("A001"));

        assertThatThrownBy(() -> missingBackendAspect.around(joinPoint, anno(method)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ICacheService");

        verify(joinPoint, never()).proceed();
    }
}
