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
import com.snowdrift.framework.schedule.xxljob.dto.XxlIJobKey;
import com.snowdrift.framework.schedule.xxljob.dto.XxlJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class XxlJobScheduleServiceImpl implements IScheduleService<XxlJobRequest, XxlIJobKey> {

    private static final String TOKEN_HEADER = "XXL-JOB-ACCESS-TOKEN";
    private static final int SUCCESS_CODE = 200;
    private static final int PAGE_SIZE = 200;

    private final XxlJobProperties properties;
    private final String[] adminUrls;
    private final Map<String, Integer> executorGroupMap = new ConcurrentHashMap<>();

    public XxlJobScheduleServiceImpl(XxlJobProperties properties) {
        this.properties = properties;
        this.adminUrls = parseAdminAddresses(properties.getAdminAddresses());
    }

    @Override
    public void addJob(XxlJobRequest request) {
        callAdminApi(XxlJobApiConst.JOB_INSERT_PATH, buildJobParam(request, getExecutorGroupId(request.getGroup())));
        log.info("XXL-JOB 任务注册成功: name={}, group={}, cron={}",
                request.getName(), request.getGroup(), request.getCron());
    }

    @Override
    public void removeJob(XxlIJobKey jobKey) {
        callAdminApi(XxlJobApiConst.JOB_DELETE_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务删除成功: id={}", jobKey.getId());
    }

    @Override
    public void pauseJob(XxlIJobKey jobKey) {
        callAdminApi(XxlJobApiConst.JOB_STOP_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务暂停: id={}", jobKey.getId());
    }

    @Override
    public void resumeJob(XxlIJobKey jobKey) {
        callAdminApi(XxlJobApiConst.JOB_START_PATH,
                Map.of("ids", JSON.toJSONString(List.of(jobKey.getId()))));
        log.info("XXL-JOB 任务恢复: id={}", jobKey.getId());
    }

    @Override
    public void triggerJob(XxlIJobKey jobKey, Map<String, Object> params) {
        Map<String, String> param = new HashMap<>();
        param.put("id", String.valueOf(jobKey.getId()));
        param.put("executorParam", JSON.toJSONString(params));
        param.put("addressList", StrConst.EMPTY);
        callAdminApi(XxlJobApiConst.JOB_TRIGGER_PATH, param);
        log.info("XXL-JOB 任务手动触发: id={}", jobKey.getId());
    }

    @Override
    public boolean exists(XxlIJobKey jobKey) {
        return findJobById(jobKey.getId()) != null;
    }

    @Override
    public JobDetails getJob(XxlIJobKey jobKey) {
        return convertToJobDetails(findJobById(jobKey.getId()));
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

            JSONObject resp = callAdminApi(XxlJobApiConst.JOB_PAGE_PATH, param);
            JSONObject content = resp.getJSONObject("content");
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

    // ========== 内部方法 ==========

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

    private String[] parseAdminAddresses(String adminAddresses) {
        if (adminAddresses == null || adminAddresses.isBlank()) return new String[0];
        return Arrays.stream(adminAddresses.split(StrConst.COMMA))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }

    private JSONObject callAdminApi(String path, Map<String, String> params) {
        if (adminUrls.length == 0) throw new BizException("schedule.xxl.admin.unreachable", new Object[]{"未配置 Admin 地址"});
        Exception lastException = null;
        for (String baseUrl : adminUrls) {
            try { return doPostApi(baseUrl, path, params); }
            catch (BizException e) { throw e; }
            catch (Exception e) { log.warn("XXL-JOB Admin[{}] 调用失败，尝试下一个: {}", baseUrl, e.getMessage()); lastException = e; }
        }
        log.error("XXL-JOB Admin 所有地址均调用失败", lastException);
        throw new BizException("schedule.xxl.admin.unreachable", new Object[]{lastException != null ? lastException.getMessage() : "unknown"});
    }

    private JSONObject callAdminGetApi(String path, Map<String, String> queryParams) {
        if (adminUrls.length == 0) throw new BizException("schedule.xxl.admin.unreachable", new Object[]{"未配置 Admin 地址"});
        Exception lastException = null;
        for (String baseUrl : adminUrls) {
            try { return doGetApi(baseUrl, path, queryParams); }
            catch (BizException e) { throw e; }
            catch (Exception e) { log.warn("XXL-JOB Admin[{}] GET 调用失败: {}", baseUrl, e.getMessage()); lastException = e; }
        }
        log.error("XXL-JOB Admin 所有地址 GET 调用均失败", lastException);
        throw new BizException("schedule.xxl.admin.unreachable", new Object[]{lastException != null ? lastException.getMessage() : "unknown"});
    }

    private JSONObject doPostApi(String baseUrl, String path, Map<String, String> params) {
        String body = HttpUtil.postForm(normalizeUrl(baseUrl) + path, params, buildAuthHeaders());
        JSONObject json = JSON.parseObject(body);
        if (json.getIntValue("code", -1) != SUCCESS_CODE) throw new BizException("schedule.xxl.api.error", new Object[]{json.getString("msg", "unknown")});
        return json;
    }

    private JSONObject doGetApi(String baseUrl, String path, Map<String, String> queryParams) {
        String url = HttpUtil.buildUrlWithParams(normalizeUrl(baseUrl) + path, queryParams);
        String body = HttpUtil.get(url, buildAuthHeaders());
        JSONObject json = JSON.parseObject(body);
        if (json.getIntValue("code", -1) != SUCCESS_CODE) throw new BizException("schedule.xxl.api.error", new Object[]{json.getString("msg", "unknown")});
        return json;
    }

    private Map<String, String> buildAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = properties.getAccessToken();
        if (StringUtils.isNotBlank(token)) headers.put(TOKEN_HEADER, token);
        return headers;
    }

    private int getExecutorGroupId(String group) {
        String appName = StringUtils.isNotBlank(group) ? group : properties.getAppName();
        Integer cached = executorGroupMap.get(appName);
        if (cached != null) return cached;

        Map<String, String> params = Map.of("offset", "0", "pagesize", "100", "appname", appName);
        JSONObject result = callAdminGetApi(XxlJobApiConst.GROUP_PAGE_PATH, params);
        JSONObject content = result.getJSONObject("content");
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

    private JSONObject findJobById(int jobId) {
        int groupId = getExecutorGroupId(properties.getAppName());
        int offset = 0;
        while (true) {
            Map<String, String> param = new HashMap<>();
            param.put("jobGroup", String.valueOf(groupId));
            param.put("offset", String.valueOf(offset));
            param.put("pagesize", String.valueOf(PAGE_SIZE));

            JSONObject result = callAdminApi(XxlJobApiConst.JOB_PAGE_PATH, param);
            JSONObject content = result.getJSONObject("content");
            if (content == null) break;
            JSONArray data = content.getJSONArray("data");
            if (data == null || data.isEmpty()) break;
            for (int i = 0; i < data.size(); i++) {
                JSONObject job = data.getJSONObject(i);
                if (job.getIntValue("id") == jobId) return job;
            }
            int total = content.getIntValue("total");
            offset += PAGE_SIZE;
            if (offset >= total) break;
        }
        return null;
    }

    private JobDetails convertToJobDetails(JSONObject job) {
        if (job == null) return null;
        JobDetails details = new JobDetails();
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

    private Map<String, Object> parseExecutorParam(String executorParam) {
        if (StringUtils.isBlank(executorParam)) return Collections.emptyMap();
        try { return JSON.parseObject(executorParam, new TypeReference<>() {}); }
        catch (Exception e) { log.warn("解析 executorParam 失败: {}", executorParam, e); return Collections.emptyMap(); }
    }

    private String normalizeUrl(String url) {
        if (StringUtils.isBlank(url)) return StrConst.EMPTY;
        return url.endsWith(StrConst.SLASH) ? url.substring(0, url.length() - 1) : url;
    }
}
