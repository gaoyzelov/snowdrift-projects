package com.snowdrift.framework.rpc.dubbo.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.Filter;

/**
 * DubboConsumerLogFilter
 *
 * @author gaoyzelov
 * @date 2026/7/9-16:33
 * @description Dubbo Consumer 端调用日志 Filter
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.CONSUMER, order = 100)
public class DubboConsumerLogFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String interfaceName = invoker.getInterface().getName();
        String methodName = invocation.getMethodName();

        try {
            Result result = invoker.invoke(invocation);
            stopWatch.stop();

            if (result.hasException()) {
                log.error("Dubbo调用异常 [Consumer] {}.{}(), elapsed={}ms",
                        interfaceName, methodName, stopWatch.getDuration().toMillis(), result.getException());
            } else {
                log.debug("Dubbo调用成功 [Consumer] {}.{}(), elapsed={}ms",
                        interfaceName, methodName, stopWatch.getDuration().toMillis());
            }
            return result;

        } catch (RpcException e) {
            stopWatch.stop();
            log.error("Dubbo调用失败 [Consumer] {}.{}(), elapsed={}ms",
                    interfaceName, methodName, stopWatch.getDuration().toMillis(), e);
            throw e;
        }
    }

}
