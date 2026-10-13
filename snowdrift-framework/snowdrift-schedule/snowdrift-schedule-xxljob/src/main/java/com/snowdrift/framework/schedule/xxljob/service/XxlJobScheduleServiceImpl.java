package com.snowdrift.framework.schedule.xxljob.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.schedule.exception.ScheduleException;
import com.snowdrift.framework.base.util.HttpUtil;
import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.model.JobDetails;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.enums.MisfireStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.config.XxlJobProperties;
import com.snowdrift.framework.schedule.xxljob.enums.BlockStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.enums.RouteStrategyEnum;
import com.snowdrift.framework.schedule.xxljob.consts.XxlJobApiConst;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobKey;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
public class XxlJobScheduleServiceImpl implements IScheduleService<XxlJobRequest, XxlJobKey> {

    private static final String TOKEN_HEADER = "XXL-JOB-ACCESS-TOKEN";
    private static final int SUCCESS_CODE = 200;
    private static final int PAGE_SIZE = 200;
    private static final int TRIGGER_STATUS_RUNNING = 1;
    private final XxlJobProperties properties;
    private final List<String> adminUrls;
    private final Cache<String, Integer> executorGroupCache = CacheBuilder.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    // 反查缓存：jobGroup 数字 ID → 执行器 AppName，用于详情/列表返回可回填的 group
    private final Cache<Integer, String> executorGroupIdCache = CacheBuilder.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    /**
     * 登录 Cookie（name=value 格式）
     */
    private volatile String loginCookie;
    private volatile long lastLoginTime;

    public XxlJobScheduleServiceImpl(XxlJobProperties properties) {
        this.properties = properties;
        this.adminUrls = parseAdminAddresses(properties.getAdminAddresses());
    }

    // ========== 任务管理 ==========

    @Override
    public XxlJobKey addJob(XxlJobRequest request) {
        int groupId = getExecutorGroupId(request.getGroup());
        JSONObject result = callAdminPostApi(XxlJobApiConst.JOB_INSERT_PATH, buildJobParam(request, groupId));
        log.info("XXL-JOB 任务注册成功: name={}, group={}, cron={}",
                request.getName(), request.getGroup(), request.getCron());

        return XxlJobKey.newInstance(result.getIntValue("data"), groupId);
    }

    @Override
    public void removeJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_DELETE_PATH,
                Map.of("ids[]", String.valueOf(jobKey.getId())));
        log.info("XXL-JOB 任务删除成功: id={}", jobKey.getId());
    }

    @Override
    public void updateJob(XxlJobKey jobKey, XxlJobRequest request) {
        int groupId = jobKey.getGroupId() != null ? jobKey.getGroupId() : getExecutorGroupId(request.getGroup());
        Map<String, String> param = buildJobParam(request, groupId);
        param.put("id", String.valueOf(jobKey.getId()));
        callAdminPostApi(XxlJobApiConst.JOB_UPDATE_PATH, param);
        // 打印实际提交的 triggerStatus：调用方未显式指定时会被默认成 1（运行），
        // 那会把已暂停的任务重新启动。日志里留痕，避免这种副作用只能靠事后翻 SQL 才发现。
        log.info("XXL-JOB 任务更新成功: id={}, name={}, cron={}, triggerStatus={}"
                        + "（未显式指定该字段时按默认运行态 1 提交，会重启已暂停的任务）",
                jobKey.getId(), request.getName(), request.getCron(), param.get("triggerStatus"));
    }

    @Override
    public void pauseJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_STOP_PATH,
                Map.of("ids[]", String.valueOf(jobKey.getId())));
        log.info("XXL-JOB 任务暂停: id={}", jobKey.getId());
    }

    @Override
    public void resumeJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_START_PATH,
                Map.of("ids[]", String.valueOf(jobKey.getId())));
        log.info("XXL-JOB 任务恢复: id={}", jobKey.getId());
    }

    @Override
    public void triggerJob(XxlJobKey jobKey, Map<String, Object> params) {
        Map<String, String> param = new HashMap<>();
        param.put("id", String.valueOf(jobKey.getId()));
        param.put("executorParam", serializeExecutorParam(params));
        param.put("addressList", StrConst.EMPTY);
        callAdminPostApi(XxlJobApiConst.JOB_TRIGGER_PATH, param);
        log.info("XXL-JOB 任务手动触发: id={}", jobKey.getId());
    }

    // ========== 查询 ==========

    @Override
    public boolean exists(XxlJobKey jobKey) {
        return Objects.nonNull(getJob(jobKey));
    }

    @Override
    public JobDetails getJob(XxlJobKey jobKey) {
        int groupId = jobKey.getGroupId() != null ? jobKey.getGroupId() : getExecutorGroupId(null);
        int offset = 0;
        while (true) {
            JSONObject content = fetchJobPage(groupId, offset);
            if (content == null) return null;
            JSONArray data = content.getJSONArray("data");
            if (data == null || data.isEmpty()) return null;
            for (int i = 0; i < data.size(); i++) {
                JSONObject job = data.getJSONObject(i);
                if (job.getIntValue("id") == jobKey.getId()) {
                    return convertToJobDetails(job);
                }
            }
            int total = content.getIntValue("total");
            offset += PAGE_SIZE;
            if (offset >= total) return null;
        }
    }

    @Override
    public List<JobDetails> listJobs() {
        return listJobs(null);
    }

    @Override
    public List<JobDetails> listJobs(String group) {
        int groupId = getExecutorGroupId(group);
        List<JobDetails> result = new ArrayList<>();
        int offset = 0;
        while (true) {
            JSONObject content = fetchJobPage(groupId, offset);
            if (content == null) break;
            JSONArray data = content.getJSONArray("data");
            if (data == null || data.isEmpty()) break;
            for (int i = 0; i < data.size(); i++) {
                result.add(convertToJobDetails(data.getJSONObject(i)));
            }
            int total = content.getIntValue("total");
            offset += PAGE_SIZE;
            if (offset >= total) break;
        }
        return result;
    }

    /**
     * 分页查询任务列表
     */
    private JSONObject fetchJobPage(int groupId, int offset) {
        Map<String, String> param = new HashMap<>();
        param.put("jobGroup", String.valueOf(groupId));
        param.put("offset", String.valueOf(offset));
        param.put("pagesize", String.valueOf(PAGE_SIZE));
        param.put("triggerStatus", "-1");
        param.put("jobDesc", StrConst.EMPTY);
        param.put("executorHandler", StrConst.EMPTY);
        param.put("author", StrConst.EMPTY);
        JSONObject resp = callAdminPostApi(XxlJobApiConst.JOB_PAGE_PATH, param);
        return resp.getJSONObject("data");
    }

    // ========== 内部方法：参数构建 ==========

    private Map<String, String> buildJobParam(XxlJobRequest request, int groupId) {
        // 统一抛 ScheduleException（与 Quartz 实现及 README 契约一致），避免 AssertUtil 抛裸 BizException
        if (StringUtils.isBlank(request.getName())) {
            throw new ScheduleException("XXL-JOB 任务名称不能为空");
        }
        if (StringUtils.isBlank(request.getCron())) {
            throw new ScheduleException("XXL-JOB Cron 表达式不能为空");
        }
        Map<String, String> param = new HashMap<>();
        param.put("jobGroup", String.valueOf(groupId));
        param.put("jobDesc", StringUtils.defaultString(request.getDescription()));
        param.put("author", StringUtils.defaultString(request.getAuthor()));
        param.put("alarmEmail", StringUtils.defaultString(request.getAlarmEmail()));
        param.put("scheduleType", "CRON");
        // cron 已由上方非空校验保证，不会再出现 null 入参
        param.put("scheduleConf", request.getCron());
        param.put("misfireStrategy", request.getMisfireStrategy() != null
                ? request.getMisfireStrategy().getCode()
                : MisfireStrategyEnum.FIRE_ONCE_NOW.getCode());
        param.put("executorRouteStrategy", request.getRouteStrategy() != null
                ? request.getRouteStrategy().getCode()
                : RouteStrategyEnum.RANDOM.getCode());
        param.put("executorHandler", request.getName());
        param.put("executorParam", serializeExecutorParam(request.getParams()));
        param.put("executorBlockStrategy", request.getBlockStrategy() != null
                ? request.getBlockStrategy().getCode()
                : BlockStrategyEnum.SERIAL_EXECUTION.getCode());
        param.put("executorTimeout", String.valueOf(request.getTimeout() != null ? request.getTimeout() : 0));
        param.put("executorFailRetryCount", String.valueOf(request.getRetryCount() != null ? request.getRetryCount() : 0));
        param.put("glueType", "BEAN");
        // 为空一律按 1（运行）处理：add 即创建后启动；update 会把已停止的任务重新启动，
        // 需要保持停止态必须由调用方显式传 0（见 XxlJobRequest#triggerStatus 的说明）。
        param.put("triggerStatus", String.valueOf(
                request.getTriggerStatus() != null ? request.getTriggerStatus() : TRIGGER_STATUS_RUNNING));
        return param;
    }

    /**
     * 序列化 executorParam：null/空 map 序列化为空串，
     * 避免 fastjson2 将 null 序列化为字面量 "null" 传给执行器
     */
    private String serializeExecutorParam(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return StrConst.EMPTY;
        }
        return JSON.toJSONString(params);
    }

    /**
     * 解析 Admin 地址列表
     */
    private List<String> parseAdminAddresses(String adminAddresses) {
        if (StringUtils.isBlank(adminAddresses)) return Collections.emptyList();
        return Arrays.stream(adminAddresses.split(StrConst.COMMA))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    // ========== 内部方法：登录 ==========

    /**
     * 确保登录态有效，cookie 过期或不存在时自动登录
     *
     * @param force 是否强制重新登录（登录过期重试场景）
     */
    private synchronized void ensureLogin(boolean force) {
        // 非强制模式下，如果登录Cookie存在且未过期，则无需重新登录
        if (!force && StringUtils.isNotBlank(loginCookie)
                && System.currentTimeMillis() - lastLoginTime < properties.getLoginTokenTimeout().toMillis()) {
            return;
        }
        if (CollectionUtils.isEmpty(adminUrls)) {
            throw new ScheduleException("XXL-JOB 未配置管理地址");
        }
        Exception lastEx = null;
        for (String baseUrl : adminUrls) {
            try {
                doLogin(baseUrl);
                return;
            } catch (ScheduleException e) {
                throw e;
            } catch (Exception e) {
                log.warn("XXL-JOB 登录失败[{}]: {}", baseUrl, e.getMessage());
                lastEx = e;
            }
        }
        if (lastEx != null) {
            throw new ScheduleException("XXL-JOB 登录失败");
        }
    }

    /**
     * 单次登录请求 — 发送凭证并从 Set-Cookie 中提取 token
     */
    private void doLogin(String baseUrl) throws Exception {
        String url = normalizeUrl(baseUrl) + XxlJobApiConst.LOGIN_PATH;
        String formBody = HttpUtil.buildFormDataBody(Map.of(
                "userName", properties.getUsername(),
                "password", properties.getPassword()
        ));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getAdminTimeout()))
                .header("Content-Type", HttpUtil.CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpUtil.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // 校验登录响应
        JSONObject json = JSON.parseObject(response.body());
        if (json.getIntValue("code", -1) != SUCCESS_CODE) {
            throw new ScheduleException("XXL-JOB 登录失败: " + json.getString("msg"));
        }

        // 从 Set-Cookie 中提取 token
        String tokenKey = properties.getLoginTokenKey();
        String cookie = response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(tokenKey + "="))
                .findFirst()
                .map(c -> c.split(";")[0])  // 仅取 "name=value" 部分
                .orElse(null);

        if (cookie == null) {
            throw new ScheduleException("XXL-JOB 登录失败: 未找到 Cookie: " + tokenKey);
        }

        this.loginCookie = cookie;
        this.lastLoginTime = System.currentTimeMillis();
        log.info("XXL-JOB 登录成功: {}", baseUrl);
    }

    // ========== 内部方法：HTTP 调用 ==========

    /**
     * API 调用器函数式接口
     */
    @FunctionalInterface
    private interface ApiCaller {
        String call(String url, Map<String, String> params, Map<String, String> headers) throws Exception;
    }

    /**
     * POST API 调用（支持多地址重试 + 登录过期自动重登）
     */
    private JSONObject callAdminPostApi(String path, Map<String, String> params) {
        return callAdminPostApi(path, params, false);
    }

    /**
     * POST API 调用（支持多地址重试 + 登录过期自动重登）
     *
     * @param isRetry 是否为登录过期后的重试调用，重试不再递归以防止无限循环
     */
    private JSONObject callAdminPostApi(String path, Map<String, String> params, boolean isRetry) {
        // Admin API 请求同样受 adminTimeout 约束（与登录请求一致）
        return callAdminApi((url, formParams, headers) ->
                HttpUtil.postForm(url, formParams, headers, Duration.ofSeconds(properties.getAdminTimeout())), path, params, isRetry);
    }

    /**
     * GET API 调用（支持多地址重试 + 登录过期自动重登）
     */
    private JSONObject callAdminGetApi(String path, Map<String, String> queryParams) {
        return callAdminGetApi(path, queryParams, false);
    }

    /**
     * GET API 调用（支持多地址重试 + 登录过期自动重登）
     *
     * @param isRetry 是否为登录过期后的重试调用，重试不再递归以防止无限循环
     */
    private JSONObject callAdminGetApi(String path, Map<String, String> queryParams, boolean isRetry) {
        // Admin API 请求同样受 adminTimeout 约束（与登录请求一致）
        return callAdminApi((url, params, headers) -> {
            String fullUrl = HttpUtil.buildUrlWithParams(url, params);
            return HttpUtil.get(fullUrl, null, headers, properties.getAdminTimeout());
        }, path, queryParams, isRetry);
    }

    /**
     * Admin API 调用通用模板（支持多地址重试 + 登录过期自动重登）
     */
    private JSONObject callAdminApi(ApiCaller caller, String path, Map<String, String> params, boolean isRetry) {
        ensureLogin(isRetry);
        Exception lastEx = null;
        for (String baseUrl : adminUrls) {
            try {
                String body = caller.call(normalizeUrl(baseUrl) + path, params, buildRequestHeaders());
                if (!isRetry && isSessionExpiredResponse(body)) {
                    log.info("XXL-JOB 会话失效（响应不是 Admin API 的响应结构），强制重登并重试: path={}", path);
                    return callAdminApi(caller, path, params, true);
                }
                return validateApiResponse(JSON.parseObject(body));
            } catch (ScheduleException e) {
                throw e;
            } catch (Exception e) {
                log.warn("XXL-JOB Admin[{}] 调用失败: {}", baseUrl, e.getMessage());
                lastEx = e;
            }
        }
        String message = lastEx != null ? lastEx.getMessage() : "unknown";
        throw new ScheduleException("XXL-JOB 管理地址不可达: " + message);
    }

    /**
     * 构建请求头
     */
    private Map<String, String> buildRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        // AccessToken 认证（OpenApi，后续兼容）
        String token = properties.getAccessToken();
        if (StringUtils.isNotBlank(token)) {
            headers.put(TOKEN_HEADER, token);
        }
        // Cookie 认证（SSO 登录）
        if (loginCookie != null) {
            headers.put("Cookie", loginCookie);
        }
        return headers;
    }

    /**
     * 判断响应是否表示「会话已失效」
     * <p>
     * 判据是<b>结构</b>而非文案：Admin API 的响应（无论成功还是业务失败）都是带 {@code code} 字段的 JSON；
     * 凡不是这个结构，就说明请求压根没进到业务方法。依据（对照本地 xxl-job 3.4.1 + xxl-sso-core 2.4.0）：
     * </p>
     * <ol>
     *   <li>{@code XxlSsoWebInterceptor#preHandle} 在未登录时，对 {@code @ResponseBody} 端点
     *       <b>抛 {@code XxlSsoException(301, …)}</b>（只有页面请求才 {@code sendRedirect} 到 loginPath）；</li>
     *   <li>xxl-job-admin 与 xxl-sso-core 均<b>未注册</b>该异常的处理器，于是它落到容器默认错误响应：</li>
     *   <li>该错误响应在无 {@code Accept} 时通常是 HTML 错误页，带 JSON 协商时是
     *       {@code {"timestamp":…,"status":500,…}} —— 两种形态都不含 {@code code} 字段。</li>
     * </ol>
     * <p>
     * 因此本方法同时覆盖这两种形态，不依赖内容协商的结果，也<b>无需也不应</b>去猜 Admin 的错误文案。
     * </p>
     * <p>
     * 反过来说：{@code code != 200} 的 <b>JSON</b> 响应一律是业务错误（cron 非法、名称重复、执行器组非法…）。
     * 原实现把「{@code code != 200} 且 msg 非空」全部判为登录过期，于是每次业务报错都会强制重登并
     * <b>重放同一次请求</b>（非幂等操作可能重复执行），还把真实错误掩盖成「登录过期」。
     * </p>
     *
     * @param body 响应体
     * @return 不是「带 code 的 JSON」时为 true
     */
    private boolean isSessionExpiredResponse(String body) {
        String trimmed = StringUtils.trimToEmpty(body);
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            // 登录页 / 错误页等非 JSON 内容
            return true;
        }
        JSONObject json = JSON.parseObject(trimmed);
        // Admin API 的响应（成功或业务失败）都带 code 字段；
        // 不带 code 的 JSON 不是 Admin API 的响应，而是容器默认错误响应的 JSON 形态
        // （{"timestamp":…,"status":500,"error":…,"path":…}）。
        return json == null || !json.containsKey("code");
    }

    /**
     * 校验 API 响应并返回，失败时抛出 BizException
     */
    private JSONObject validateApiResponse(JSONObject json) {
        if (json.getIntValue("code", -1) != SUCCESS_CODE) {
            throw new ScheduleException("XXL-JOB API 调用失败: " + json.getString("msg", "unknown"));
        }
        return json;
    }

    // ========== 内部方法：分组 & 任务查找 ==========

    private int getExecutorGroupId(String group) {
        String appName = StringUtils.isNotBlank(group) ? group : properties.getAppName();
        try {
            return executorGroupCache.get(appName, () -> loadExecutorGroupId(appName));
        } catch (ExecutionException | UncheckedExecutionException e) {
            // Guava 将 loader 抛出的受检异常包装为 ExecutionException、运行时异常（含 ScheduleException）
            // 包装为 UncheckedExecutionException，二者都需捕获并还原，避免裸包装异常外泄破坏统一异常契约
            if (e.getCause() instanceof ScheduleException) {
                throw (ScheduleException) e.getCause();
            }
            throw new ScheduleException("XXL-JOB 分组未找到: " + appName);
        }
    }

    private int loadExecutorGroupId(String appName) {
        Map<String, String> params = Map.of("offset", "0", "pagesize", "100", "appname", appName);
        JSONObject result = callAdminGetApi(XxlJobApiConst.GROUP_PAGE_PATH, params);
        JSONObject content = result.getJSONObject("data");
        if (content != null) {
            JSONArray data = content.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject gi = data.getJSONObject(i);
                    if (appName.equals(gi.getString("appname"))) {
                        int groupId = gi.getIntValue("id");
                        executorGroupIdCache.put(groupId, appName);
                        return groupId;
                    }
                }
            }
        }
        throw new ScheduleException("XXL-JOB 分组未找到: " + appName);
    }

    /**
     * 根据 jobGroup 数字 ID 反查执行器 AppName
     * <p>
     * 供 {@link #convertToJobDetails} 返回与入参语义一致的 group（AppName），
     * 使查询结果可回填 listJobs(group) 等操作。反查失败时降级返回数字 ID，保持旧行为。
     * </p>
     */
    private String getExecutorAppName(int groupId) {
        try {
            return executorGroupIdCache.get(groupId, () -> loadExecutorAppName(groupId));
        } catch (ExecutionException | UncheckedExecutionException e) {
            // 同 getExecutorGroupId：ScheduleException 会被 Guava 包装为 UncheckedExecutionException，需一并捕获才能命中兜底
            log.warn("XXL-JOB 解析执行器 AppName 失败，使用 jobGroup 数字 ID 兜底: groupId={}", groupId);
            return String.valueOf(groupId);
        }
    }

    private String loadExecutorAppName(int groupId) {
        Map<String, String> params = Map.of("offset", "0", "pagesize", "100");
        JSONObject result = callAdminGetApi(XxlJobApiConst.GROUP_PAGE_PATH, params);
        JSONObject content = result.getJSONObject("data");
        if (content != null) {
            JSONArray data = content.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject gi = data.getJSONObject(i);
                    if (gi.getIntValue("id") == groupId) {
                        String appName = gi.getString("appname");
                        if (StringUtils.isNotBlank(appName)) {
                            executorGroupCache.put(appName, groupId);
                            return appName;
                        }
                    }
                }
            }
        }
        throw new ScheduleException("XXL-JOB 执行器分组未找到: " + groupId);
    }


    private JobDetails convertToJobDetails(JSONObject job) {
        if (job == null) return null;
        JobDetails details = new JobDetails();
        details.setJobKey(XxlJobKey.newInstance(job.getIntValue("id"), job.getIntValue("jobGroup")));
        details.setName(job.getString("executorHandler"));
        details.setGroup(getExecutorAppName(job.getIntValue("jobGroup")));
        details.setCron(job.getString("scheduleConf"));
        details.setDescription(job.getString("jobDesc"));
        details.setStatus(job.getIntValue("triggerStatus") == 1 ? JobStatusEnum.NORMAL : JobStatusEnum.PAUSED);
        details.setParams(parseExecutorParam(job.getString("executorParam")));
        long lastTime = job.getLongValue("triggerLastTime", 0L);
        details.setLastFireTime(lastTime > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTime), ZoneId.systemDefault()) : null);
        long nextTime = job.getLongValue("triggerNextTime", 0L);
        details.setNextFireTime(nextTime > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTime), ZoneId.systemDefault()) : null);
        return details;
    }

    /**
     * 解析执行器参数
     */
    private Map<String, Object> parseExecutorParam(String executorParam) {
        if (StringUtils.isBlank(executorParam)) return Collections.emptyMap();
        try {
            return JSON.parseObject(executorParam, new TypeReference<>() {
            });
        } catch (RuntimeException e) {
            log.warn("解析 executorParam 失败: {}", executorParam, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 规范化 URL
     */
    private String normalizeUrl(String url) {
        if (StringUtils.isBlank(url)) return StrConst.EMPTY;
        return url.endsWith(StrConst.SLASH) ? url.substring(0, url.length() - 1) : url;
    }
}
