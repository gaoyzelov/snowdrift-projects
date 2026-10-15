# Snowdrift Social DingTalk

提供钉钉企业内部应用 Token、用户 OAuth 授权及 Stream 事件接入能力。模块是供下游服务引入的基础库，无需新增应用启动类。

配置来源、租户与应用选择由下游负责；组件不加载数据库、不维护全局活动配置，不自动同步集群配置，也不缓存或刷新 Token。

## 引入与配置

使用 Snowdrift BOM/父 POM 管理版本后，引入：

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-social-dingtalk</artifactId>
</dependency>
```

Spring Boot 自动发现模块配置。基础服务与 Stream 分别启用，默认均关闭，没有顶层总开关。

```yaml
snowdrift:
  social:
    dingtalk:
      base:
        enabled: true
        app-key: ${DINGTALK_APP_KEY}
        app-secret: ${DINGTALK_APP_SECRET}
        oauth-redirect-uri: https://example.com/oauth/callback
      event-listener:
        enabled: true
        client-id: ${DINGTALK_STREAM_CLIENT_ID}
        client-secret: ${DINGTALK_STREAM_CLIENT_SECRET}
```

| 配置前缀 `snowdrift.social.dingtalk` 下的属性 | 说明 |
| --- | --- |
| `base.enabled` | 是否自动注册默认 `IDingTalkService`，默认关闭 |
| `base.app-key` / `base.app-secret` | 默认应用凭据；启用基础服务时必须非空 |
| `base.oauth-redirect-uri` | 生成授权 URL 时必填，须是带主机名的绝对 HTTP/HTTPS 地址 |
| `event-listener.enabled` | 是否创建并启动默认 Stream 客户端，默认关闭 |
| `event-listener.client-id` / `event-listener.client-secret` | 默认 Stream 客户端凭据；启用监听时必须非空 |

仅使用基础服务时省略 `event-listener`；仅监听事件时省略 `base`，并注册事件处理器。未开启 `base` 时组件不会注册 `IDingTalkService`，手动工厂仍可使用。

下游提供 `IDingTalkService` Bean 时默认基础服务退让。下游提供 `OpenDingTalkClient` Bean 时默认 Stream 客户端退让，其凭据校验、启动和关闭均由下游负责。

## 使用默认服务

启用 `base` 后直接注入 `IDingTalkService`。默认服务使用创建时的配置副本，不随配置对象的后续修改自动变化，也不会注册到静态工厂。

```java
import com.snowdrift.social.dingtalk.model.AppAccessTokenVO;
import com.snowdrift.social.dingtalk.model.UserAccessTokenVO;
import com.snowdrift.social.dingtalk.service.IDingTalkService;

// dingTalkService 为下游通过构造器注入的 IDingTalkService。
AppAccessTokenVO appToken = dingTalkService.getAppAccessToken();
String authorizeUrl = dingTalkService.getUserOauthUrl(state);
UserAccessTokenVO userToken = dingTalkService.getUserAccessToken(authCode);
```

`getAppAccessToken()` 获取应用 Token；`getUserOauthUrl(state)` 生成用户授权 URL；`getUserAccessToken(authCode)` 用回调授权码换取用户 Token。服务创建时校验应用凭据；回调地址仅在生成授权 URL 时校验，因此只获取应用 Token 时可不配置回调地址。

Token 获取每次都会请求钉钉。响应成功且包含非空 Token、正数有效期才返回结果。`AppAccessTokenVO.expiresIn` 和 `UserAccessTokenVO.expireIn` 的单位均为秒。用户 Token 的刷新令牌由 VO 返回，模块暂不提供刷新接口。下游缓存须按应用隔离，结合有效期提前失效，并在凭据更新时清理。

SDK 调用失败时抛出固定消息的 `BizException`，保留原始 `cause`；模块只记录操作、配置标识和异常类型。配置与 Token 对象的 `toString()` 排除密钥/令牌，但 JSON 序列化仍保留业务所需字段，不要直接记录其 JSON 或对外序列化异常链。

## 多应用与手动工厂

`DingTalkSocialConfig` 是普通配置 DTO，可由下游从数据库、配置中心或业务逻辑构造。工厂是静态工具，无需注入 Spring Bean，也不依赖 `base.enabled`。

```java
import com.snowdrift.social.dingtalk.properties.DingTalkSocialConfig;
import com.snowdrift.social.dingtalk.service.DingTalkServiceFactory;
import com.snowdrift.social.dingtalk.service.IDingTalkService;

DingTalkSocialConfig config = new DingTalkSocialConfig(
        "tenant-a", appKey, appSecret, "https://example.com/tenant-a/callback");
IDingTalkService service = DingTalkServiceFactory.create(config);
IDingTalkService sameService = DingTalkServiceFactory.get("tenant-a");
boolean exists = DingTalkServiceFactory.contains("tenant-a");

// 不需要全局缓存时，也可直接 new DingTalkServiceImpl(config)。
```

| 工厂方法 | 行为 |
| --- | --- |
| `create(config)` | 创建并缓存实例；同一标识重复或并发创建返回已有实例，首次成功创建的配置生效 |
| `reload(config)` | 构建并校验新实例后原子替换映射，返回新实例；不存在则注册，失败保留旧实例 |
| `get(configKey)` | 获取已有实例，未创建时返回 `null` |
| `contains(configKey)` | 检查实例是否存在 |
| `remove(configKey)` | 移除缓存项并返回旧实例，不存在时返回 `null` |

所有方法拒绝空配置标识；`create` 和 `reload` 还拒绝空配置，构建新实例时须有有效凭据。创建失败不会占用标识。服务复制传入的配置，创建完成后修改原 DTO 不会改变实例；不要在调用创建或更新方法期间由其他线程修改同一个 DTO。相同标识已存在时，`create` 不会应用新 DTO 中的凭据。

工厂缓存按 JVM/类加载器共享，不是数据库或集群注册中心。下游应使用能够区分租户和应用的标识，并在实例不再需要时调用 `remove`。默认 Spring 服务和工厂同名实例相互独立；移除工厂项不会改变默认服务或已持有的旧实例引用。

更新凭据时直接调用 `DingTalkServiceFactory.reload(newConfig)`，使用配置中的 `configKey` 定位实例，无需先 `remove`。新实例构建期间旧实例仍可读取；构建成功后通过 `ConcurrentHashMap.put` 原子替换，后续 `get` 返回新实例。已持有的旧实例引用继续使用旧配置，默认 Spring 服务也不受影响。并发更新以最后执行替换的实例为准，不按请求开始时间或配置版本排序。

`create`、`reload`、`get`、`remove` 的单次映射操作由并发 Map 保证线程安全，不额外使用全局 `synchronized`。更新期间显式并发 `remove` 仍可能移除该标识；`contains` 后再 `get` 等组合操作也不是原子操作。工厂不保证跨实例同步或旧版本覆盖防护，这些由下游协调。OAuth 授权跨越配置版本时，可使用带版本的新标识，并在旧授权完成后清理旧标识。

## OAuth 回调与应用关联

下游应保证一次授权开始与回调使用同一应用。组件仅编码透传 `state`，不生成、存储或验证授权上下文。以下为下游实现的流程示意，`authorizationStateStore` 表示业务提供的一次性上下文存储：

```java
// 授权开始：确定应用，生成不可预测的 state，保存短期且绑定会话的关联。
String configKey = "tenant-a";
String state = java.util.UUID.randomUUID().toString();
authorizationStateStore.save(state, configKey, currentSessionId);
IDingTalkService service = DingTalkServiceFactory.get(configKey);
if (service == null) {
    throw new IllegalStateException("应用尚未初始化");
}
String authorizeUrl = service.getUserOauthUrl(state);

// 回调：校验 state、有效期和会话，原子消费后按原标识获取服务。
String originalKey = authorizationStateStore.validateAndConsume(state, currentSessionId);
IDingTalkService originalService = DingTalkServiceFactory.get(originalKey);
if (originalService == null) {
    throw new IllegalStateException("应用已移除，请重新授权");
}
UserAccessTokenVO userToken = originalService.getUserAccessToken(authCode);
```

多实例部署时，下游负责各实例的服务初始化，并使用共享授权上下文存储。一次授权期间应保留对应配置版本，避免同一标识被替换为另一个应用；原配置不可用时重新发起授权，不能自动回退到其他应用。回调地址还须与钉钉应用登记配置一致。

## Stream 事件处理

启用 `event-listener` 后，至少提供一个 `IEventHandler` Bean。每个处理器声明唯一且非空的事件类型；缺少处理器、重复类型或缺少客户端凭据会导致启动失败。Stream 不依赖基础服务或工厂中的应用，工厂实例变更不会重启 Stream。

```java
import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.social.dingtalk.event.IEventHandler;
import org.springframework.stereotype.Component;

@Component
public class ExampleDingTalkEventHandler implements IEventHandler {
    @Override
    public String supportType() {
        return "bpms_instance_change";
    }

    @Override
    public void handle(String eventId, JSONObject data) {
        // 根据 eventId 做幂等控制；data 可能为空。
    }
}
```

SDK 的 shaded JSON 转换为项目使用的 Fastjson2 `JSONObject` 后传入处理器。处理成功返回成功确认；处理器异常或数据转换失败返回稍后重试；未知或空事件类型记录告警后确认忽略。处理器应按可能重复、并发投递设计幂等和线程安全逻辑。

默认客户端在容器启动时启动、销毁时停止；启动失败会尝试清理，清理失败作为 suppressed exception 保留。重复关闭不会重复停止客户端。

## 迁移与验证

本次重构将默认应用配置移入 `base`，原顶层 `enabled`、`app-key`、`app-secret`、`oauth-redirect-uri` 及 `configs`、`active-config-key` 不再参与装配。请迁移到上述新格式，不能仅保留旧总开关。

旧 Provider、配置快照、Registry 和活动门面已移除。原数据库配置加载及活动应用选择移至下游；多应用调用改用 `DingTalkServiceFactory.create/get`。实现类和客户端工具分别使用 `DingTalkServiceImpl`、`DingTalkClientUtil`。

```bash
mvn -B -ntp clean test -pl snowdrift-framework/snowdrift-social/snowdrift-social-dingtalk -am
```

测试覆盖两个能力的独立与组合装配、自定义 Bean 退让、配置转换与脱敏、工厂并发创建、原子更新和清理、OAuth 编码、Token 响应与异常处理、事件确认/重试及客户端生命周期。SDK 客户端均以 Mock 替代，无需真实凭据，不会连接钉钉。
