# snowdrift-mq

基于**各 broker 原生客户端**（spring-kafka / Spring AMQP / rocketmq-spring）的统一消息队列模块，提供 Kafka / RocketMQ / RabbitMQ 的自动配置与通用发送门面。

设计要点：

- **发送统一**：`IMqService` 屏蔽三种 MQ 差异（同步 / 异步 / 延迟 / 批量），由各实现模块用原生客户端实现；
- **消费原生**：不提供统一监听注解，直接使用各 broker 原生注解（`@KafkaListener` / `@RabbitListener` / `@RocketMQMessageListener`）；框架以**容器级钩子**自动恢复/清理上下文（见「上下文传播」）；
- **无 Spring Cloud Stream**：不再依赖 StreamBridge / SCS binder。

## 模块结构

```
snowdrift-mq
├── snowdrift-mq-base        ← 通用层：IMqService、序列化、拦截器、上下文传播、参数校验
├── snowdrift-mq-kafka       ← 原生 spring-kafka 自动配置
├── snowdrift-mq-rabbitmq    ← 原生 Spring AMQP（spring-boot-starter-amqp）自动配置
└── snowdrift-mq-rocketmq    ← 原生 rocketmq-spring-boot-starter 自动配置
```

## 快速开始

按需引入**一个**实现模块即可，`snowdrift-mq-base` 会作为传递依赖自动引入：

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-mq-kafka</artifactId>
</dependency>
<!-- 或 snowdrift-mq-rabbitmq / snowdrift-mq-rocketmq（同一时间只启用一种） -->
```

## 配置

基础开关默认开启，各实现模块需显式 `enabled=true` 才会装配 `IMqService` 与上下文钩子；**broker 连接参数直接用该 broker 自己的 Spring Boot 配置**，不再二次封装。

```yaml
snowdrift:
  mq:
    # 基础开关，默认 true（依赖 base 时保持开启）
    # enabled: true
    # sign: false            # 是否启用消息 HMAC-SHA256 签名
    # sign-key: xxx          # sign=true 时必填，否则启动失败
    executor:
      core-size: 4           # 异步发送线程池
      max-size: 8
      queue-capacity: 100

    # 启用 Kafka（连接用 spring.kafka.*）
    kafka:
      enabled: true

    # 启用 RabbitMQ（连接用 spring.rabbitmq.*）
    rabbitmq:
      enabled: false
      delay-plugin-enabled: false   # 是否启用 delayed-message-exchange 插件

    # 启用 RocketMQ（连接用 rocketmq.name-server / rocketmq.producer.group）
    rocketmq:
      enabled: false
```

```yaml
# broker 原生连接配置示例（Kafka）
spring:
  kafka:
    bootstrap-servers: localhost:9092

# RabbitMQ
# spring.rabbitmq.host/port/username/password
# RocketMQ（rocketmq-spring-boot-starter 前缀）
# rocketmq.name-server: localhost:9876
# rocketmq.producer.group: snowdrift-producer
```

> 同一时间只允许启用一种实现模块；若同时把 `kafka`/`rabbitmq`/`rocketmq` 置为 `enabled=true`，启动将失败（`MqBinderActivationGuard`）。

## 消息发送

注入 `IMqService` 即可：

```java
@Autowired
private IMqService mqService;

// 1. 同步发送
mqService.send("order-paid", orderEvent);

// 2. 带 Key 发送（Kafka 分区键 / RocketMQ 分片键）
mqService.send("order-paid", "order-123", orderEvent);

// 3. 带自定义 Header
mqService.send("order-paid", "order-123", orderEvent, Map.of("biz", "pay"));

// 4. 异步发送
CompletableFuture<MqSendResult> future = mqService.sendAsync("order-paid", orderEvent);

// 5. 批量发送（逐条，非原子；失败抛出 MqException）
List<MqMessage<OrderEvent>> batch = List.of(
    MqMessage.<OrderEvent>builder().payload(event1).key("1").build(),
    MqMessage.<OrderEvent>builder().payload(event2).key("2").build());
List<MqSendResult> results = mqService.sendBatch("order-topic", batch);
```

`IMqService` 的少参数版本均为接口 `default` 便捷委托，最终落到「全参数」抽象方法；`sendBatch` 默认逐条调用 `send`。

### 各 broker 语义与元数据

| 能力 | Kafka | RabbitMQ | RocketMQ |
|------|-------|----------|----------|
| `topic` | topic | **exchange** | topic（可用 `RocketMQHeaders.TAGS` 或 `topic:tag` 追加 tag） |
| `key` | 分区键 | routing key（缺省 `""`） | RocketMQ keys（分片/去重键） |
| `messageId` | `topic-partition-offset` | 自生成 UUID | `SendResult.msgId` |
| `partitionOrQueue` | partition | 无（null） | queueId |
| `sendAsync` | 原生异步 | 基类 executor 包装 | 原生回调 |

### 延迟消息

| MQ | 行为 |
|----|------|
| RocketMQ | 原生延迟级别（1~18），`Duration` 就近向上映射（见 `RocketDelayLevels`），超 2h 钳制到 18 |
| RabbitMQ | 依赖 delayed-message-exchange 插件：需 `delay-plugin-enabled=true` 且发送目标 exchange 为 delayed-exchange；否则抛 `UnsupportedOperationException` |
| Kafka | 无原生延迟能力，抛 `UnsupportedOperationException` |

```java
mqService.sendDelay("order-timeout", orderEvent, Duration.ofSeconds(30));
```

## 消息消费（原生注解）

框架**不提供**统一监听注解，请按 broker 使用各自原生监听注解；业务方法内无需任何上下文样板代码（框架在容器级自动恢复/清理）。

```java
// Kafka
@Component
public class OrderKafkaListener {
    @KafkaListener(topics = "order-paid", groupId = "order-service")
    public void onOrderPaid(OrderPaidEvent event) { /* ... */ }
}

// RabbitMQ：topic 为 exchange 时，监听的是绑定到该 exchange 的 queue
@Component
public class OrderRabbitListener {
    @RabbitListener(queues = "order-paid-queue")
    public void onOrderPaid(OrderPaidEvent event) { /* ... */ }
}

// RocketMQ
@Component
@RocketMQMessageListener(consumerGroup = "order-service", topic = "order-paid", selectorExpression = "PAID")
public class OrderRocketListener implements RocketMQListener<OrderPaidEvent> {
    @Override
    public void onMessage(OrderPaidEvent event) { /* ... */ }
}
```

> Payload 反序列化：发送端将消息体序列化为 JSON 字节；消费端可用 broker 的 deserializer 还原，或监听方法接收原始类型后借助 `MqMessageConverter` 转换。RocketMQ 如需取原始属性（如恢复上下文的手动用法），监听参数可用 `MessageExt`。

## 上下文传播

Producer 发送时自动写入以下消息头（broker 无关的 `x-snowdrift-*` 约定），Consumer 端由框架钩子**自动恢复并在调用结束后清理**，签名校验失败则拒绝消费该消息：

| 传播内容 | Header Key | 来源 |
|---------|-----------|------|
| 链路追踪 ID | `x-snowdrift-trace-id` | MDC `traceId` |
| 消息 Key | `x-snowdrift-message-key` | 发送时传入的 key |
| 用户 ID | `x-snowdrift-user-id` | `SecurityContextHolder` |
| 登录账号 | `x-snowdrift-username` | `SecurityContextHolder` |
| 租户 ID | `x-snowdrift-tenant-id` | `SecurityContextHolder` |
| 部门 ID | `x-snowdrift-dept-id` | `SecurityContextHolder` |
| 消息签名 | `x-snowdrift-signature` | HMAC-SHA256（`sign=true` 时启用，密钥必填） |

容器级自动恢复的实现：

| MQ | 钩子 |
|----|------|
| Kafka | 监听容器工厂 `RecordInterceptor` |
| RabbitMQ | 监听容器工厂 `adviceChain`（`MqRabbitContextAdvice`） |
| RocketMQ | 监听容器 Bean 代理拦截 `handleMessage(MessageExt)` |

## 序列化切换

默认 FastJson2（`String`/`byte[]`/POJO 往返对称），注册一个 `MqMessageConverter` Bean 即可全局覆盖：

```java
@Component
public class JacksonMqMessageConverter implements MqMessageConverter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(Object payload) {
        try {
            return MAPPER.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("MQ 序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> targetType) {
        try {
            return MAPPER.readValue(data, targetType);
        } catch (IOException e) {
            throw new IllegalArgumentException("MQ 反序列化失败", e);
        }
    }
}
```

## 拦截器链

实现 `MqSendInterceptor` 并注册为 Spring Bean 即自动生效（按 `getPriority()` 降序，同优先级按注册先后）：

```java
@Component
public class MessageAuditInterceptor implements MqSendInterceptor {
    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void beforeSend(String topic, String key, Object payload) { /* ... */ }

    @Override
    public void afterSend(String topic, MqSendResult result) { /* ... */ }

    @Override
    public void onSendError(String topic, Throwable ex) { /* ... */ }
}
```

运行时动态增删（`MqInterceptorRegistry` 为 Spring Bean）：

```java
registry.register(new CustomInterceptor());
registry.unregister(someInterceptor);
```

## 配置属性参考

### snowdrift.mq（核心，base）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `true` | 总开关 |
| `executor.core-size` | int | `4` | 异步发送线程池核心线程数 |
| `executor.max-size` | int | `8` | 最大线程数 |
| `executor.queue-capacity` | int | `100` | 队列容量 |
| `executor.keep-alive-seconds` | int | `60` | 线程存活时间 |
| `executor.thread-name-prefix` | String | `snowdrift-mq-async-` | 线程名前缀 |
| `executor.wait-for-tasks-to-complete-on-shutdown` | boolean | `true` | 关闭时等待任务完成 |
| `executor.await-termination-seconds` | int | `30` | 关闭等待超时 |
| `sign` | Boolean | `false` | 是否启用消息 HMAC-SHA256 签名 |
| `sign-key` | String | — | 签名密钥（`sign=true` 时必填，否则启动失败） |

### snowdrift.mq.kafka

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `false` | 启用 Kafka 实现；连接参数用 `spring.kafka.*` |

### snowdrift.mq.rabbitmq

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `false` | 启用 RabbitMQ 实现；连接参数用 `spring.rabbitmq.*` |
| `delay-plugin-enabled` | Boolean | `false` | 是否启用 delayed-message-exchange 插件（`x-delay`） |

### snowdrift.mq.rocketmq

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `false` | 启用 RocketMQ 实现；连接参数用 `rocketmq.name-server` 等（starter 前缀） |

## SPI 扩展点

| 接口 | 用途 | 注册方式 |
|------|------|---------|
| `MqMessageConverter` | 序列化 / 反序列化 | 注册 Bean 覆盖默认 FastJson2 |
| `MqSendInterceptor` | 发送前后自定义逻辑 | 注册为 Spring Bean（支持优先级排序） |
| `MqInterceptorRegistry` | 运行时动态管理拦截器 | 注入后调用 `register()` / `unregister()` |

## 架构分层

```
用户代码          │  mqService.send()        │  @KafkaListener / @RabbitListener /
                  │                         │  @RocketMQMessageListener
──────────────────┼─────────────────────────┼────────────────────────────────
snowdrift-mq base │  MqSendInterceptor 链 · MqMessageConverter · 上下文传播/签名
                  │  IMqService 门面（参数校验 · 异步 · 批量 · 延迟默认拒绝）
──────────────────┼─────────────────────────┼────────────────────────────────
                  │  原生客户端              │  原生消费 + 容器级上下文恢复
                  ├──────────┬───────────┬──┴────────────────────────────────
                  │  Kafka   │ RocketMQ  │  RabbitMQ
                  └──────────┴───────────┴───────────────────────────────────
```

## 兼容性说明

- **RocketMQ**：`rocketmq-spring-boot-starter` 在模块内定点 `2.3.5`。该 starter 面向 Boot 2.7/Spring 5.3 构建，在 Boot 3.5 下可编译、自动发现与（经字节码核对）消费拦截成立，但**运行时兼容需真实 broker 联调验证**。
- 各实现模块的消费上下文自动恢复依赖 broker 库的容器钩子行为，建议在真实中间件环境做一次冒烟。
- 本模块不再依赖 Spring Cloud Stream；旧版基于 `@MqListener`/StreamBridge 的用法已废弃。
