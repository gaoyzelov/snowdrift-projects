package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.base.exception.BizException;
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
 * @date 2026/7/9-16:32
 * @description Dubbo Provider 端异常处理 Filter
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

            // 必须放在所有「直接传播」判断之前：Dubbo 内置 ExceptionFilter（order=0，比本过滤器更靠内层）
            // 对「未在方法签名声明、且与接口不在同一 codebase」的异常会执行
            //   appResponse.setException(new RuntimeException(StringUtils.toString(原异常)))
            // 而 StringUtils.toString 会把原异常类名与完整堆栈打进 message。本过滤器在外层收到它时，
            // 异常已是 java.lang.RuntimeException（JDK 类，getCodeBase 为 null），会被后续任一判断放行，
            // 导致内部包名/文件行号随响应泄露给调用方 —— 正是本过滤器存在的意义。
            if (isStackTraceLeakingWrapper(exception)) {
                log.error("Dubbo RPC调用异常（已剥离内置包装携带的堆栈），调用方：{}，服务名: {}，方法: {}",
                        RpcContext.getServiceContext().getRemoteHost(),
                        invoker.getInterface().getName(),
                        invocation.getMethodName(), exception);
                appResponse.setException(new BizException("RPC 调用异常"));
                return;
            }

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
            if (className.startsWith("java.") || className.startsWith("javax.")
                    || className.startsWith("jakarta.")) {
                return;
            }

            // Dubbo 自身异常直接抛出
            if (exception instanceof RpcException) {
                return;
            }

            // BizException 直接抛出（Consumer 端必然有 snowdrift-base 依赖）
            if (exception instanceof BizException) {
                return;
            }

            // 不可反序列化的异常，打印日志并包装
            log.error("Dubbo RPC调用异常，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                    RpcContext.getServiceContext().getRemoteHost(),
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    exception.getLocalizedMessage(), exception);

            appResponse.setException(new BizException("RPC 调用异常"));

        } catch (Throwable e) {
            log.error("Dubbo RPC异常处理出错，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                    RpcContext.getServiceContext().getRemoteHost(),
                    invoker.getInterface().getName(),
                    invocation.getMethodName(),
                    e.getLocalizedMessage(), e);
            appResponse.setException(new BizException("RPC 调用异常"));
        }
    }

    /**
     * 判断异常是否已被 Dubbo 内置 {@code ExceptionFilter} 包装过
     * <p>该过滤器产出的是<b>恰好</b> {@link RuntimeException} 实例，消息为
     * {@code StringUtils.toString(原异常)}，形如 {@code "原异常类名: 说明\n\tat ..."}。
     * 业务代码自己抛的 {@code RuntimeException} 消息里几乎不可能出现制表符缩进的堆栈行，
     * 因此“类完全相同 + 消息含 {@code "\tat "}”可作为该包装的可靠指纹。</p>
     *
     * @param exception 待判定异常
     * @return 命中内置包装指纹时返回 {@code true}
     */
    private static boolean isStackTraceLeakingWrapper(Throwable exception) {
        if (exception.getClass() != RuntimeException.class) {
            return false;
        }
        String message = exception.getMessage();
        return message != null && message.contains("\tat ");
    }

    @Override
    public void onError(Throwable e, Invoker<?> invoker, Invocation invocation) {
        log.error("Dubbo RPC调用异常，调用方：{}，服务名: {}，方法: {}, 异常信息: {}",
                RpcContext.getServiceContext().getRemoteHost(),
                invoker.getInterface().getName(),
                invocation.getMethodName(),
                e.getLocalizedMessage(), e);
    }

}
