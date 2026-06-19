package com.snowdrift.framework.schedule.xxljob.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.HttpUtil;
import com.snowdrift.framework.schedule.core.IScheduleService;
import com.snowdrift.framework.schedule.dto.JobDetails;
import com.snowdrift.framework.schedule.enums.JobStatusEnum;
import com.snowdrift.framework.schedule.xxljob.config.XxlJobProperties;
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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class XxlJobScheduleServiceImpl implements IScheduleService<XxlJobRequest, XxlJobKey> {

    private static final String TOKEN_HEADER = "XXL-JOB-ACCESS-TOKEN";
    private static final int SUCCESS_CODE = 200;
    private static final int PAGE_SIZE = 200;
    private final XxlJobProperties properties;
    private final List<String> adminUrls;
    private final Map<String, Integer> executorGroupMap = new ConcurrentHashMap<>();

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
        return XxlJobKey.newInstance(result.getIntValue("id"), groupId);
    }

    @Override
    public void removeJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_DELETE_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务删除成功: id={}", jobKey.getId());
    }

    @Override
    public void pauseJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_STOP_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务暂停: id={}", jobKey.getId());
    }

    @Override
    public void resumeJob(XxlJobKey jobKey) {
        callAdminPostApi(XxlJobApiConst.JOB_START_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务恢复: id={}", jobKey.getId());
    }

    @Override
    public void triggerJob(XxlJobKey jobKey, Map<String, Object> params) {
        Map<String, String> param = new HashMap<>();
        param.put("id", String.valueOf(jobKey.getId()));
        param.put("executorParam", JSON.toJSONString(params));
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
        Map<String, String> param = new HashMap<>();
        param.put("jobGroup", String.valueOf(groupId));
        param.put("offset", "0");
        param.put("pagesize", String.valueOf(PAGE_SIZE));
        param.put("triggerStatus", "-1");
        param.put("jobDesc", StrConst.EMPTY);
        param.put("executorHandler", StrConst.EMPTY);
        param.put("author", StrConst.EMPTY);

        JSONObject resp = callAdminPostApi(XxlJobApiConst.JOB_PAGE_PATH, param);
        JSONObject content = resp.getJSONObject("data");
        if (content == null) return null;
        JSONArray data = content.getJSONArray("data");
        if (data == null) return null;
        for (int i = 0; i < data.size(); i++) {
            JSONObject job = data.getJSONObject(i);
            if (job.getIntValue("id") == jobKey.getId()) {
                return convertToJobDetails(job);
            }
        }
        return null;
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
            Map<String, String> param = new HashMap<>();
            param.put("jobGroup", String.valueOf(groupId));
            param.put("offset", String.valueOf(offset));
            param.put("pagesize", String.valueOf(PAGE_SIZE));
            param.put("triggerStatus","-1");
            param.put("jobDesc", StrConst.EMPTY);
            param.put("executorHandler", StrConst.EMPTY);
            param.put("author", StrConst.EMPTY);

            JSONObject resp = callAdminPostApi(XxlJobApiConst.JOB_PAGE_PATH, param);
            JSONObject content = resp.getJSONObject("data");
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

    // ========== 内部方法：参数构建 ==========

    private Map<String, String> buildJobParam(XxlJobRequest request, int groupId) {
        Map<String, String> param = new HashMap<>();
        param.put("jobGroup", String.valueOf(groupId));
        param.put("jobDesc", request.getDescription());
        param.put("author", request.getAuthor());
        param.put("alarmEmail", request.getAlarmEmail());
        param.put("scheduleType", "CRON");
        param.put("scheduleConf", request.getCron());
        param.put("misfireStrategy", request.getMisfireStrategy().getCode());
        param.put("executorRouteStrategy", request.getRouteStrategy().getCode());
        param.put("executorHandler", request.getName());
        param.put("executorParam", JSON.toJSONString(request.getParams()));
        param.put("executorBlockStrategy", request.getBlockStrategy().getCode());
        param.put("executorTimeout", String.valueOf(request.getTimeout()));
        param.put("executorFailRetryCount", String.valueOf(request.getRetryCount()));
        param.put("glueType", "BEAN");
        param.put("triggerStatus", "1");
        return param;
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
            throw new BizException("schedule.xxl.admin.unreachable", new Object[]{"未配置 Admin 地址"});
        }
        Exception lastEx = null;
        for (String baseUrl : adminUrls) {
            try {
                doLogin(baseUrl);
                return;
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("XXL-JOB 登录失败[{}]: {}", baseUrl, e.getMessage());
                lastEx = e;
            }
        }
        if (lastEx != null) {
            throw new BizException("schedule.xxl.login.failed", new Object[]{lastEx.getMessage()});
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
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", HttpUtil.CONTENT_TYPE_FORM)
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpUtil.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // 校验登录响应
        JSONObject json = JSON.parseObject(response.body());
        if (json.getIntValue("code", -1) != SUCCESS_CODE) {
            throw new BizException("schedule.xxl.login.failed", new Object[]{json.getString("msg")});
        }

        // 从 Set-Cookie 中提取 token
        String tokenKey = properties.getLoginTokenKey();
        String cookie = response.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(tokenKey + "="))
                .findFirst()
                .map(c -> c.split(";")[0])  // 仅取 "name=value" 部分
                .orElse(null);

        if (cookie == null) {
            throw new BizException("schedule.xxl.login.failed", new Object[]{"未找到 Cookie: " + tokenKey});
        }

        this.loginCookie = cookie;
        this.lastLoginTime = System.currentTimeMillis();
        log.info("XXL-JOB 登录成功: {}", baseUrl);
    }

    // ========== 内部方法：HTTP 调用 ==========

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
        ensureLogin(isRetry);
        Exception lastEx = null;
        for (String baseUrl : adminUrls) {
            try {
                String body = HttpUtil.postForm(normalizeUrl(baseUrl) + path, params, buildRequestHeaders());
                JSONObject json = JSON.parseObject(body);
                if (isLoginExpired(json) && !isRetry) {
                    log.info("XXL-JOB 登录过期，强制重登并重试: path={}", path);
                    return callAdminPostApi(path, params, true);
                }
                return validateApiResponse(json);
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("XXL-JOB Admin[{}] 调用失败: {}", baseUrl, e.getMessage());
                lastEx = e;
            }
        }
        String message = lastEx != null ? lastEx.getMessage() : "unknown";
        throw new BizException("schedule.xxl.admin.unreachable", new Object[]{message});
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
        ensureLogin(isRetry);
        Exception lastEx = null;
        for (String baseUrl : adminUrls) {
            try {
                String url = HttpUtil.buildUrlWithParams(normalizeUrl(baseUrl) + path, queryParams);
                String body = HttpUtil.get(url, buildRequestHeaders());
                JSONObject json = JSON.parseObject(body);
                if (isLoginExpired(json) && !isRetry) {
                    log.info("XXL-JOB 登录过期，强制重登并重试: path={}", path);
                    return callAdminGetApi(path, queryParams, true);
                }
                return validateApiResponse(json);
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.warn("XXL-JOB Admin[{}] GET 调用失败: {}", baseUrl, e.getMessage());
                lastEx = e;
            }
        }
        String message = lastEx != null ? lastEx.getMessage() : "unknown";
        throw new BizException("schedule.xxl.admin.unreachable", new Object[]{message});
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
     * 判断 API 响应是否为登录过期
     * <p>
     * XXL-JOB Admin 未登录或登录过期时返回 code!=200，
     * </p>
     */
    private boolean isLoginExpired(JSONObject json) {
        if (json == null || json.getIntValue("code", -1) == SUCCESS_CODE) {
            return false;
        }
        String msg = json.getString("msg");
        if (StringUtils.isBlank(msg)) {
            return false;
        }
        log.info("XXL-JOB 登录过期: {}", msg);
        return true;
    }

    /**
     * 校验 API 响应并返回，失败时抛出 BizException
     */
    private JSONObject validateApiResponse(JSONObject json) {
        if (json.getIntValue("code", -1) != SUCCESS_CODE) {
            throw new BizException("schedule.xxl.api.error", new Object[]{json.getString("msg", "unknown")});
        }
        return json;
    }

    // ========== 内部方法：分组 & 任务查找 ==========

    private int getExecutorGroupId(String group) {
        String appName = StringUtils.isNotBlank(group) ? group : properties.getAppName();
        Integer cached = executorGroupMap.get(appName);
        if (cached != null) return cached;

        Map<String, String> params = Map.of("offset", "0", "pagesize", "100", "appname", appName);
        JSONObject result = callAdminGetApi(XxlJobApiConst.GROUP_PAGE_PATH, params);
        JSONObject content = result.getJSONObject("data");
        if (content != null) {
            JSONArray data = content.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject gi = data.getJSONObject(i);
                    if (appName.equals(gi.getString("appname"))) {
                        int id = gi.getIntValue("id");
                        executorGroupMap.put(appName, id);
                        return id;
                    }
                }
            }
        }
        throw new BizException("schedule.xxl.group.not.found", new Object[]{appName});
    }


    private JobDetails convertToJobDetails(JSONObject job) {
        if (job == null) return null;
        JobDetails details = new JobDetails();
        details.setJobKey(XxlJobKey.newInstance(job.getIntValue("id"), job.getIntValue("jobGroup")));
        details.setName(job.getString("executorHandler"));
        details.setGroup(String.valueOf(job.getIntValue("jobGroup")));
        details.setCron(job.getString("scheduleConf"));
        details.setDescription(job.getString("jobDesc"));
        details.setStatus(job.getIntValue("triggerStatus") == 1 ? JobStatusEnum.NORMAL : JobStatusEnum.ERROR);
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
        } catch (Exception e) {
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
