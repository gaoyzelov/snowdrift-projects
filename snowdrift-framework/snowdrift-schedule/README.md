# snowdrift-schedule

基于 Quartz / XXL-Job 的统一定时任务调度模块，通过 `IScheduleService` 抽象 API 屏蔽后端差异。

## 模块结构

```
snowdrift-schedule
├── snowdrift-schedule-core        ← 通用层：IScheduleService、JobInfo、JobStatus
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
      default-group: DEFAULT

    # XXL-Job
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: snowdrift-app
      access-token: ""
      username: admin
      password: 123456
```

## 代码示例

```java
// 动态注册任务
QuartzJobKey key = scheduleService.addJob(QuartzJobRequest.builder()
    .name("cleanJob")
    .group("maintenance")
    .cron("0 0 2 * * ?")
    .jobClass(CleanTask.class)
    .build());

// 查询任务
JobDetails job = scheduleService.getJob(key);

// 暂停/恢复
scheduleService.pauseJob(key);
scheduleService.resumeJob(key);

// 手动触发
scheduleService.triggerJob(key, Map.of("force", true));

// 删除
scheduleService.removeJob(key);
```

## 后端对比

| 特性 | Quartz | XXL-Job |
|------|--------|---------|
| 部署模式 | 嵌入应用 | 独立 Admin 服务 |
| 分布式调度 | 需 JDBC JobStore | 原生支持 |
| 任务管理 UI | 无 | Admin 控制台 |
| 失败重试 | 支持 | 支持 |
| 动态注册 | 支持 | 支持（通过 Admin API） |

## 扩展

实现 `IScheduleService<T, K>` 接口即可接入其他调度平台。
