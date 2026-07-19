# snowdrift-mq

基于 Spring Cloud Stream 的统一消息队列模块，提供 Kafka / RocketMQ / RabbitMQ 的自动配置和通用抽象。

## 模块结构

```
snowdrift-mq
├── snowdrift-mq-core        ← 通用层：IMqService、@MqListener、SPI、上下文传播
├── snowdrift-mq-kafka       ← Kafka binder 自动配置
├── snowdrift-mq-rocketmq    ← RocketMQ binder 自动配置
└── snowdrift-mq-rabbitmq    ← RabbitMQ binder 自动配置
```

## 快速开始

按需引入一个 MQ binder 即可，核心 API（`snowdrift-mq-core`）会作为传递依赖自动引入。

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-mq-kafka</artifactId>
</dependency>
<!-- 或 mq-rocketmq / mq-rabbitmq -->
```

## 配置

```yaml
snowdrift:
  mq:
    enabled: true               # 默认 true
    executor:
      core-size: 4              # 异步发送线程池
      max-size: 8
      queue-capacity: 100

    # Kafka
    kafka:
      enabled: true
      bootstrap-servers: localhost:9092
      acks: all
      compression-type: lz4

    # RocketMQ
    rocketmq:
      enabled: false
      name-server: localhost:9876
      producer-group: ${spring.application.name}-producer

    # RabbitMQ
    rabbitmq:
      enabled: false
      addresses: localhost:5672
      virtual-host: /
      delay-plugin-enabled: false  # 是否启用 x-delay 插件
```

> `snowdrift.mq.*` 属性会自动映射到 `spring.cloud.stream.*`，仅当用户未显式配置 SCS 属性时才生效。直接用 SCS 原生配置也可。

## 消息发送

注入 `IMqService` 即可使用：

```java
@Autowired
private IMqService mqService;

// 1. 同步发送
mqService.send("order-paid", orderEvent);

// 2. 带 Key 发送（用于分区 / 分片路由）
mqService.send("order-paid", "order-123", orderEvent);

// 3. 带自定义 Header
Map<String, String> headers = Map.of("x-trace-id", traceId);
mqService.send("order-paid", "order-123", orderEvent, headers);

// 4. 异步发送
CompletableFuture<MqSendResult> future = mqService.sendAsync("order-paid", orderEvent);
future.thenAccept(result -> log.info("发送完成: {}", result.getTopic()));

// 5. 批量发送
List<MqMessage<OrderEvent>> batch = List.of(
    MqMessage.<OrderEvent>builder().payload(event1).key("1").build(),
    MqMessage.<OrderEvent>builder().payload(event2).key("2").build()
);
List<MqSendResult> results = mqService.sendBatch("order-topic", batch);
```

### 延迟消息

```java
// 30 秒后投递
mqService.sendDelay("order-timeout", orderEvent, Duration.ofSeconds(30));
```

| MQ | 实现方式 | 说明 |
|----|---------|------|
| RocketMQ | 原生延迟级别 1-18 | Duration 自动映射到最近的上限级别 |
| RabbitMQ | x-delay 插件 或 x-message-ttl + DLX | 按 `delay-plugin-enabled` 选择策略 |
| Kafka | 降级为即时发送 | 输出 WARN 日志 |

## 消息消费

```java
@Component
public class OrderEventListener {

    @MqListener(topic = "order-paid", group = "order-service")
    public void onOrderPaid(OrderPaidEvent event) {
        // SecurityContext.getUserId() / getTenantId() 自动恢复为发送方的值
        orderService.process(event);
    }

    @MqListener(topic = "order-cancel", group = "order-service", maxRetry = 5, concurrency = 3)
    public void onOrderCancel(OrderCancelEvent event) {
        orderService.cancel(event);
    }
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `topic` | — | 监听的 destination |
| `group` | `""` | 消费组名（Kafka / RocketMQ 生效） |
| `maxRetry` | `3` | 最大重试次数，0=不重试 |
| `concurrency` | `1` | 消费线程并发数 |
| `autoCommit` | `false` | 是否自动提交 offset（Kafka） |

> `@MqListener` 方法由框架自动包装为 Spring Cloud Stream Consumer Bean，无需手动配置 binding。

## 序列化切换

默认 FastJson2，切换为 Jackson：

```java
@Component
public class JacksonMqMessageConverter implements MqMessageConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(Object payload) {
        return MAPPER.writeValueAsBytes(payload);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> targetType) {
        return MAPPER.readValue(data, targetType);
    }
}
```

注册同名 Bean 即可全局覆盖。

## 拦截器链

### 基本用法

```java
@Component
public class MessageAuditInterceptor implements MqSendInterceptor {

    @Override
    public int getPriority() {
        return 100; // 越大越先执行，建议分段：加密 200、审计 100、通知 0
    }

    @Override
    public void beforeSend(String topic, String key, Object payload) {
        log.info("发送前审计: topic={}, payload={}", topic, payload);
    }

    @Override
    public void afterSend(String topic, MqSendResult result) {
        log.info("发送后审计: topic={}, result={}", topic, result);
    }

    @Override
    public void onSendError(String topic, Throwable ex) {
        log.error("发送失败审计: topic={}", topic, ex);
    }
}
```

实现 `MqSendInterceptor` 接口并注册为 Spring Bean 即可自动生效。

### 运行时动态管理

```java
@Autowired
private MqInterceptorRegistry registry;

// 热添加
registry.register(new CustomInterceptor());

// 热移除
registry.unregister(someInterceptor);
```

## 上下文传播

Producer 端发送时，以下上下文自动注入消息头，Consumer 端 `@MqListener` 方法执行前自动恢复：

| 传播内容 | Header Key | 来源 |
|---------|-----------|------|
| 链路追踪 ID | `x-snowdrift-trace-id` | MDC `traceId` |
| 用户 ID | `x-snowdrift-user-id` | `SecurityContextHolder` |
| 登录账号 | `x-snowdrift-username` | `SecurityContextHolder` |
| 租户 ID | `x-snowdrift-tenant-id` | `SecurityContextHolder` |

Consumer 端在 `@MqListener` 方法执行完毕后自动清除上下文，避免线程池污染。

## 配置属性参考

### snowdrift.mq（核心）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `true` | 总开关 |
| `dynamic-destination-cache-size` | Integer | `10` | StreamBridge 动态 destination 缓存大小 |
| `executor.core-size` | int | `4` | 异步发送线程池核心线程数 |
| `executor.max-size` | int | `8` | 最大线程数 |
| `executor.queue-capacity` | int | `100` | 队列容量 |
| `executor.keep-alive-seconds` | int | `60` | 线程存活时间 |
| `executor.thread-name-prefix` | String | `snowdrift-mq-async-` | 线程名前缀 |

### snowdrift.mq.kafka

| 属性 | 类型 | 默认值 | SCS 映射 |
|------|------|--------|---------|
| `enabled` | Boolean | `true` | — |
| `bootstrap-servers` | String | `localhost:9092` | → `spring.cloud.stream.kafka.binder.brokers` |
| `acks` | String | `1` | → `spring.cloud.stream.kafka.binder.required-acks` |
| `compression-type` | String | `none` | → `spring.cloud.stream.kafka.binder.configuration.compression.type` |

### snowdrift.mq.rocketmq

| 属性 | 类型 | 默认值 | SCS 映射 |
|------|------|--------|---------|
| `enabled` | Boolean | `true` | — |
| `name-server` | String | `localhost:9876` | → `spring.cloud.stream.rocketmq.binder.name-server` |
| `producer-group` | String | `snowdrift-producer` | → `spring.cloud.stream.rocketmq.binder.producer.group` |
| `consumer-group` | String | `snowdrift-consumer` | → `spring.cloud.stream.rocketmq.binder.consumer.group` |

### snowdrift.mq.rabbitmq

| 属性 | 类型 | 默认值 | SCS 映射 |
|------|------|--------|---------|
| `enabled` | Boolean | `true` | — |
| `addresses` | String | `localhost:5672` | → `spring.cloud.stream.rabbit.binder.addresses` |
| `virtual-host` | String | `/` | → `spring.cloud.stream.rabbit.binder.virtual-host` |
| `username` | String | `guest` | → `spring.cloud.stream.rabbit.binder.username` |
| `password` | String | `guest` | → `spring.cloud.stream.rabbit.binder.password` |
| `delay-plugin-enabled` | Boolean | `false` | 启用 rabbitmq-delayed-message-exchange 插件 |

## SPI 扩展点

| 接口 | 用途 | 注册方式 |
|------|------|---------|
| `MqMessageConverter` | 序列化 / 反序列化 | 注册同名 Bean 覆盖默认 FastJson2 |
| `MqSendInterceptor` | 发送前后自定义逻辑 | 注册为 Spring Bean（支持优先级排序） |
| `MqInterceptorRegistry` | 运行时动态管理拦截器 | 注入后调用 `register()` / `unregister()` |

## 架构分层

```
用户代码        │  @MqListener    │  mqService.send()   │
────────────────┼─────────────────┼─────────────────────┤
snowdrift-mq API│  MqSendInterceptor 链                 │
                │  MqMessageConverter (SPI)             │
                │  MqContextPropagator (TTL)            │
────────────────┼─────────────────┼─────────────────────┤
                │      Spring Cloud Stream              │
                ├──────────┬───────────┬────────────────┤
                │  Kafka   │ RocketMQ  │  RabbitMQ      │
                └──────────┴───────────┴────────────────┘
```

> 用户可随时绕过 snowdrift-mq，直接使用 `StreamBridge` / `@Bean Consumer` 等 Spring Cloud Stream 原生 API。
