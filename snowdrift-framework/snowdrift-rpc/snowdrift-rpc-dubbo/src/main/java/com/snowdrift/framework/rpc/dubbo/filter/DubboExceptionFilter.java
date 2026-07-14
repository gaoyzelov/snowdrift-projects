package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.service.GenericService;

import java.lang.reflect.Method;

/**
 * DubboExceptionFilter
 *
 * @author gaoyzelov
 * @description Dubbo Provider 端异常处理 Filter
 * @date 2026/7/9 16:32
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = -100)
public class DubboExceptionFilter implements Filter, Filter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (!appResponse.hasException()) {
            return;
        }
        // 泛化调用不做处理
        if (GenericService.class == invoker.getInterface()) {
            return;
        }

        try {
            Throwable exception = appResponse.getException();

            // 非运行时异常且为 Exception 类型，直接抛出
            if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
                return;
            }

            // 方法签名上声明的异常直接抛出
            try {
                Method method = invoker.getInterface().getMethod(invocation.getMethodName(),
                        invocation.getParameterTypes());
                Class<?>[] exceptionClasses = method.getExceptionTypes();
                for (Class<?> exceptionClass : exceptionClasses) {
                    if (exceptionClass.isInstance(exception)) {
                        return;
                    }
                }
            } catch (NoSuchMethodException e) {
                return;
            }

            // 异常类和接口类在同一个 jar 包中，直接抛出
            String serviceFile = ReflectUtils.getCodeBase(invoker.getInterface());
            String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());
            if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)) {
                return;
            }

            // JDK 异常直接抛出
            String className = exception.getClass().getName();
            if (className.startsWith("java.") || className.startsWith("javax.")) {
                return;
            }

            // Dubbo 自身异常直接抛出
            if (exception instanceof RpcException) {
                return;
            }

            // BizException 直接抛出（Consumer 端必然有 snowdrift-common 依赖）
            if (exception instanceof BizException) {
                return;
            }

            // 不可反序列化的异常，打印日志并包装
            log.error("RPC调用异常，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                    RpcContext.getServiceContext().getRemoteHost(),
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    exception.getLocalizedMessage(), exception);

            appResponse.setException(new BizException(exception.getLocalizedMessage()));

        } catch (Throwable e) {
            log.error("RPC异常处理出错，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                    RpcContext.getServiceContext().getRemoteHost(),
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    e.getLocalizedMessage(), e);
            appResponse.setException(new BizException(e.getLocalizedMessage(),e));
        }
    }

    @Override
    public void onError(Throwable e, Invoker<?> invoker, Invocation invocation) {
        log.error("RPC调用异常，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                RpcContext.getServiceContext().getRemoteHost(),
                invoker.getInterface().getName(),
                invocation.getMethodName(),
                e.getLocalizedMessage(), e);
    }

}
