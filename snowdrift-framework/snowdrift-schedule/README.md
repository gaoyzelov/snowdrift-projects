# snowdrift-schedule

基于 Quartz / XXL-Job 的统一定时任务调度模块，通过 `IScheduleService` 抽象 API 屏蔽后端差异。

## 模块结构

```
snowdrift-schedule
├── snowdrift-schedule-core        ← 通用层：IScheduleService、JobRequest、JobDetails、JobStatusEnum
├── snowdrift-schedule-quartz      ← Quartz 本地调度
└── snowdrift-schedule-xxljob      ← XXL-Job 分布式调度
```

## 快速开始

按需引入一个调度后端即可，核心 API（`snowdrift-schedule-core`）会作为传递依赖自动引入。

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-schedule-quartz</artifactId>
</dependency>
<!-- 或 schedule-xxljob -->
```

## 配置

```yaml
snowdrift:
  schedule:
    # Quartz
    quartz:
      enabled: true               # 默认 true

    # XXL-Job
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: snowdrift-job     # 默认 "snowdrift-job"
      username: admin
      password: 123456
      access-token: ""
      login-token-key: XXL_JOB_LOGIN_IDENTITY
      login-token-timeout: 30m
      admin-timeout: 5            # Admin API 超时（秒）
      ip:                         # 执行器 IP（留空自动探测）
      port: 9999                  # 执行器端口
      log-path:                   # 日志路径
      log-retention-days: 30      # 日志保留天数
```

## 代码示例

### 动态注册任务

QuartzJobRequest 没有 `@Builder`，使用 setter 方式构造：

```java
// Quartz 任务
QuartzJobRequest request = new QuartzJobRequest();
request.setName("cleanJob");
request.setGroup("maintenance");
request.setCron("0 0 2 * * ?");
request.setDescription("每日凌晨清理");
request.setJobClass(CleanTask.class);
QuartzJobKey key = scheduleService.addJob(request);

// XXL-JOB 任务
XxlJobRequest request = new XxlJobRequest();
request.setName("myHandler");
request.setCron("0 */5 * * * ?");
request.setGroup("default");
request.setDescription("每 5 分钟执行");
XxlJobKey key = scheduleService.addJob(request);
```

### 任务管理（统一 API）

所有方法通过 `IJobKey` 统一标识任务，Quartz 用 `name+group`，XXL-JOB 用 `id+groupId`。

```java
// 查询任务
JobDetails job = scheduleService.getJob(key);
boolean exists = scheduleService.exists(key);
List<JobDetails> all = scheduleService.listJobs();
List<JobDetails> groupList = scheduleService.listJobs("default");

// 更新任务
XxlJobRequest updateReq = new XxlJobRequest();
updateReq.setCron("0 0 3 * * ?");
scheduleService.updateJob(key, updateReq);

// 暂停/恢复/触发
scheduleService.pauseJob(key);
scheduleService.resumeJob(key);
scheduleService.triggerJob(key, Map.of("param", "value"));

// 删除
scheduleService.removeJob(key);
```

### 任务数据模型

| 类 | 字段 | 说明 |
|----|------|------|
| `JobRequest` | name, group, cron, description, params, misfireStrategy | 公共字段 |
| `QuartzJobRequest` | 继承 + jobClass | Quartz 独有：指定 Job 实现类 |
| `XxlJobRequest` | 继承 + author, alarmEmail, routeStrategy, blockStrategy, timeout, retryCount | XXL-JOB 独有：路由策略、超时等 |
| `JobDetails` | jobKey, name, group, cron, description, status, params, lastFireTime, nextFireTime | 任务详情 |
| `QuartzJobKey` | name, group | Quartz 任务标识 |
| `XxlJobKey` | id, groupId | XXL-JOB 任务标识 |

`MisfireStrategyEnum` 默认值为 `FIRE_ONCE_NOW`（错过触发时间后立即执行一次）。

## 后端对比

| 特性 | Quartz | XXL-Job |
|------|--------|---------|
| 部署模式 | 嵌入应用 | 独立 Admin 服务 |
| 分布式调度 | 需 JDBC JobStore | 原生支持 |
| 任务管理 UI | 无 | Admin 控制台 |
| 失败重试 | 支持 | 支持 |
| 动态注册 | 支持 | 支持（通过 Admin API） |
| 多 Admin 故障转移 | — | 支持（配置多个地址逗号分隔） |

## 扩展

实现 `IScheduleService<T, K>` 接口即可接入其他调度平台。
