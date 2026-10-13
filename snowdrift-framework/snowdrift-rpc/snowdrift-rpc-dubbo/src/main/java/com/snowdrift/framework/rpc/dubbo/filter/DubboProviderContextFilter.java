package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.context.http.HttpContextHolder;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.rpc.dubbo.constant.RpcContextConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * DubboProviderContextFilter
 *
 * @author gaoyzelov
 * @date 2026/7/9-16:34
 * @description Dubbo Provider 端上下文恢复 Filter
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = -200)
public class DubboProviderContextFilter implements Filter, Filter.Listener {

    private static final String TRACE_ID_KEY = StrConst.TRACE_ID;

    /**
     * 是否信任调用方通过 RPC 附件传来的 {@code SecurityContext}
     * <p><b>默认 false。</b>Dubbo 附件完全由调用方控制，任何能连到 Provider 端口的一方都能塞入任意
     * {@code userId/tenantId/roleKeys}。默认不恢复身份，只恢复 traceId，避免把身份认定权交给对端。</p>
     * <p>仅当调用方与提供方之间存在网络层可信隔离（mTLS / 服务网格 / 严格 ACL），
     * 或已由网关注入并校验过身份时才可置为 {@code true}。可通过 JVM 参数
     * {@code -Dsnowdrift.rpc.dubbo.trust-inbound-security-context=true}
     * 或同名环境变量 {@code SNOWDRIFT_RPC_DUBBO_TRUST_INBOUND_SECURITY_CONTEXT=true} 开启。</p>
     */
    public static final String TRUST_INBOUND_SECURITY_CONTEXT_PROPERTY =
            "snowdrift.rpc.dubbo.trust-inbound-security-context";

    /**
     * 是否信任入站身份；每次调用读取，便于运行期调整与测试
     */
    static boolean trustInboundSecurityContext() {
        String value = System.getProperty(TRUST_INBOUND_SECURITY_CONTEXT_PROPERTY);
        if (StringUtils.isBlank(value)) {
            value = System.getenv("SNOWDRIFT_RPC_DUBBO_TRUST_INBOUND_SECURITY_CONTEXT");
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        restoreContext();
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        clearContext();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        clearContext();
    }

    /**
     * 从 Dubbo attachment 恢复上下文
     * <p>显式使用 {@link RpcContext#getServerAttachment()}（Dubbo 3 提供端读取上游入站 attachment 的推荐通道，
     * 框架 {@code ContextFilter} 将入站 attachment 放入其中），不依赖 {@code getServiceContext()}
     * 标注 {@code @Experimental} 的兼容委托。</p>
     */
    private void restoreContext() {
        RpcContext rpcContext = RpcContext.getServerAttachment();

        // 检查 Consumer 端上下文注入失败标记
        String contextError = rpcContext.getAttachment(RpcContextConstants.CONTEXT_ERROR);
        if (StrConst.TRUE.equals(contextError)) {
            log.warn("Dubbo 服务端上下文注入失败，跳过上下文恢复");
            return;
        }

        // 恢复 TraceId
        String traceId = (String) rpcContext.getObjectAttachment(RpcContextConstants.TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
        }

        // 恢复安全上下文：附件由调用方完全控制，默认不信任
        SecurityContext ctx = (SecurityContext) rpcContext.getObjectAttachment(RpcContextConstants.SECURITY_CONTEXT);
        if (ctx == null) {
            return;
        }
        if (!trustInboundSecurityContext()) {
            log.warn("忽略调用方传来的 SecurityContext（userId={}）：RPC 附件可被调用方任意伪造。"
                            + "如已通过 mTLS/服务网格/网关校验保证调用方可信，可设置 -D{}=true 开启恢复",
                    ctx.getUserId(), TRUST_INBOUND_SECURITY_CONTEXT_PROPERTY);
            return;
        }
        SecurityContextHolder.setContext(ctx);
    }

    /**
     * 清除上下文，避免线程池复用污染
     * <p>
     * 三个上下文都要清。{@code HttpContextHolder} 用的是 {@code TransmittableThreadLocal}（继承自
     * {@code InheritableThreadLocal}）：Dubbo Provider 线程若在某个 HTTP 请求处理期间首次创建，
     * 会永久继承那个请求的 {@code HttpContext}，此前这里只清 Security 与 traceId，导致该线程后续
     * 读到的是<b>另一个请求</b>的 IP/URI（审计与日志切面会据此写错归属）。
     * </p>
     */
    private void clearContext() {
        SecurityContextHolder.clear();
        MDC.remove(TRACE_ID_KEY);
        HttpContextHolder.clear();
    }

}
