# Snowdrift Social DingTalk

钉钉 Stream 事件与 OAuth 接入模块。引入本模块后，Spring Boot 会自动加载钉钉配置；默认不开启。

```yaml
snowdrift:
  social:
    dingtalk:
      enabled: true
      active-config-key: primary
      configs:
        primary:
          enabled: true
          app-key: ${DINGTALK_APP_KEY}
          app-secret: ${DINGTALK_APP_SECRET}
          oauth-redirect-uri: https://example.com/oauth/callback
        secondary:
          enabled: true
          app-key: ${DINGTALK_SECONDARY_APP_KEY}
          app-secret: ${DINGTALK_SECONDARY_APP_SECRET}
          oauth-redirect-uri: https://example.com/secondary/oauth/callback
      event-listener:
        enabled: true
        client-id: ${DINGTALK_CLIENT_ID}
        client-secret: ${DINGTALK_CLIENT_SECRET}
```

`IDingTalkService` 是稳定门面，默认转发到 `active-config-key` 指定的配置；现有的注入与调用代码无需变化。需要显式选择一个应用时，可注入 `DingTalkServiceRegistry` 并调用 `getService("secondary")`。运行时切换可调用 `activate("secondary")`，后续通过门面发起的调用将使用新配置。

旧版单配置仍兼容：`app-key`、`app-secret`、`oauth-redirect-uri` 可直接写在 `snowdrift.social.dingtalk` 下；未配置 `configs` 时会映射为 `default`。如果同时配置新旧两种格式，`configs` 优先。

### 独立使用 Stream

Stream 凭据与服务配置相互独立，仅使用事件监听时无需配置 `configs` 或旧版服务凭据：

```yaml
snowdrift:
  social:
    dingtalk:
      enabled: true
      event-listener:
        enabled: true
        client-id: ${DINGTALK_CLIENT_ID}
        client-secret: ${DINGTALK_CLIENT_SECRET}
```

未配置任何服务应用时，Registry 和 `IDingTalkService` 门面仍会注册，活动配置为空；调用服务方法时抛出“当前没有激活的钉钉服务配置”。存在应用配置时，凭据及活动配置必须有效；组件不会自动选择第一个应用。显式启用 Stream 时必须提供非空客户端凭据和至少一个 `IEventHandler` Bean，缺失会导致启动失败。如果下游提供自己的 `OpenDingTalkClient` Bean，默认客户端退让，由下游管理其启停。

### OAuth 与 Token

`getAppAccessToken()` 获取企业内部应用 Token；`getUserOauthUrl(state)` 生成用户授权地址；`getUserAccessToken(authCode)` 用回调授权码换取用户 Token。只获取 Token 的应用无需配置回调地址；生成授权地址时，`app-key` 必须非空，`oauth-redirect-uri` 必须是带主机名的绝对 HTTP/HTTPS 地址，并与钉钉应用登记的回调地址一致。

多配置场景下，一次授权必须固定使用同一 `configKey`。默认门面每次调用都读取当前活动配置，如果授权期间切换活动应用，回调不能直接依赖门面选择配置。下面的流程由下游控制器/服务实现，`authorizationStateStore` 表示下游提供的授权上下文存储：

```java
// 授权开始：生成不可预测的 state，并以短有效期保存一次性关联。
String configKey = registry.getActiveConfigKey();
String state = java.util.UUID.randomUUID().toString();
authorizationStateStore.save(state, configKey, currentSessionId);
String url = registry.getService(configKey).getUserOauthUrl(state);
// 将浏览器重定向至 url。

// 回调：校验 state、有效期及发起授权的会话，并原子消费一次性关联。
String originalConfigKey = authorizationStateStore.validateAndConsume(state, currentSessionId);
UserAccessTokenVO token = registry.getService(originalConfigKey).getUserAccessToken(authCode);
```

组件只透传 `state`，不生成、存储或校验授权上下文。存储应支持一次性原子消费；多实例部署时使用共享存储。原配置被删除或禁用后，回调会失败，应重新发起授权，不能回退到其他应用。

Token 获取每次都会请求钉钉，组件不缓存 Token，也不提供用户 Token 刷新接口。下游如需缓存，应按应用配置隔离，结合响应有效期提前失效，并在凭据变化时清除旧 Token。`AppAccessTokenVO.expiresIn` 与 `UserAccessTokenVO.expireIn` 均以秒为单位。Token 对象的 `toString()` 排除令牌字段，JSON 序列化仍保留字段供调用方使用，因此不要直接记录其 JSON。

SDK 请求失败时抛出固定消息的 `BizException`，原始原因保留在 `cause` 中；模块日志不直接输出 SDK 异常消息或堆栈。下游对外返回错误时也应避免序列化原始异常链。

### 数据库配置

组件不依赖数据库或 ORM。下游应用可提供自己的 `IDingTalkConfigProvider` Bean：

- `load()` 返回包含所有应用配置和唯一 `activeConfigKey` 的完整一致快照。
- `activate(configKey)` 在数据库中持久化激活项。组件在持久化成功后切换当前实例的活动策略。
- 数据发生变化后，在各应用实例调用 `DingTalkServiceRegistry.refresh()`；也可以通过消息通知或定时校准实现集群同步。

数据库 Provider 存在时会覆盖默认的配置文件 Provider；两种来源不会自动合并。每个实例会在启动时加载并校验完整快照，刷新失败会保留上一份有效快照。多实例场景应以数据库中的唯一激活项作为事实来源，并自行广播激活变更或安排轮询；仅切换单实例内存状态不能保证集群一致。

服务配置的 `app-secret` 不应输出到日志。配置文件中的凭据建议使用环境变量或密钥管理系统注入。

启用 Stream 监听后，需要在 Spring 容器中提供 `IEventHandler` Bean。每个处理器通过 `supportType()` 声明一个唯一的钉钉事件类型，并在 `handle()` 中处理事件。事件数据以项目依赖管理的 Fastjson2 `JSONObject` 传入，可能为空；SDK 返回的 shaded JSON 对象会在模块内部转换。

```java
import com.snowdrift.social.dingtalk.event.IEventHandler;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class ExampleDingTalkEventHandler implements IEventHandler {

    @Override
    public String supportType() {
        return "bpms_instance_change";
    }

    @Override
    public void handle(String eventId, JSONObject data) {
        // 按事件类型解析并处理 data
    }
}
```

处理器抛出异常时，模块向 Stream SDK 返回稍后重试；未注册的事件类型会记录告警并确认成功，不会重试。下游处理器应根据 `eventId` 做幂等控制，并按可能并发投递设计线程安全逻辑；避免把客户端密钥写入日志或源码配置。

Stream 监听器仍独立于 OAuth 服务配置：上述数据库策略切换不会重启或切换 Stream 客户端。
