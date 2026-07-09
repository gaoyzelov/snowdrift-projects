package com.snowdrift.framework.rpc.dubbo.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.Filter;

/**
 * DubboProviderLogFilter
 *
 * @author gaoyzelov
 * @description Dubbo Provider 端调用日志 Filter
 * @date 2026/7/9 16:34
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = 100)
public class DubboProviderLogFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        String remoteHost = RpcContext.getServiceContext().getRemoteHost();
        String interfaceName = invoker.getInterface().getName();
        String methodName = invocation.getMethodName();

        try {
            Result result = invoker.invoke(invocation);
            long elapsed = System.currentTimeMillis() - start;

            if (result.hasException()) {
                log.error("Dubbo服务异常 [Provider] {}.{}(), caller={}, elapsed={}ms",
                        interfaceName, methodName, remoteHost, elapsed, result.getException());
            } else {
                log.debug("Dubbo服务成功 [Provider] {}.{}(), caller={}, elapsed={}ms",
                        interfaceName, methodName, remoteHost, elapsed);
            }
            return result;

        } catch (RpcException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Dubbo服务失败 [Provider] {}.{}(), caller={}, elapsed={}ms",
                    interfaceName, methodName, remoteHost, elapsed, e);
            throw e;
        }
    }

}
