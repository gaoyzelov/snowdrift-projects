package com.snowdrift.framework.security.satoken.interceptor;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import com.snowdrift.framework.base.result.ResultCode;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.annotation.Anonymous;
import com.snowdrift.framework.security.exception.SecurityException;
import com.snowdrift.framework.security.service.ISecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 安全拦截器
 * <p>
 * 继承 Sa-Token 的 {@link SaInterceptor}，在其认证流程之上叠加：
 * <ol>
 *   <li>{@link Anonymous} 注解跳过认证</li>
 *   <li>兼容 Sa-Token 自身的注解鉴权</li>
 *   <li>通过 {@link ISecurityService} 校验登录状态</li>
 *   <li>将 {@link SecurityContext} 写入 {@link SecurityContextHolder}，供后续业务代码获取</li>
 * </ol>
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/15
 * @since 1.0.0
 */
public class SecurityInterceptor extends SaInterceptor {

    private final ISecurityService securityService;

    /**
     * 通过构造注入安全服务实现，解耦底层框架
     *
     * @param securityService 安全服务实现（如 SaTokenSecurityServiceImpl）
     */
    public SecurityInterceptor(ISecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非 HandlerMethod 的处理器（静态资源 ResourceHttpRequestHandler、RouterFunction 端点、
        // BeanNameUrlHandlerMapping 的 handler）同样会被 DispatcherServlet 分发，也注册了本拦截器，
        // 因此同样必须校验登录。只有 @Anonymous 能放行，且它只允许标注在方法上。
        Method method = handler instanceof HandlerMethod handlerMethod ? handlerMethod.getMethod() : null;

        // 1. 标记了 @Anonymous 的方法直接放行
        if (Objects.nonNull(method) && method.isAnnotationPresent(Anonymous.class)) {
            return true;
        }

        // 2. 调用安全服务校验登录状态（对全部处理器类型生效）
        securityService.checkLogin();

        if (Objects.isNull(method)) {
            // 没有可检视的方法，也就没有方法级上下文/注解需要处理；登录校验已在上方完成
            return true;
        }

        // 3. 从会话中捞出 SecurityContext 并放入 ThreadLocal，供后续业务代码使用
        SecurityContext sc = securityService.getContext();
        if (Objects.isNull(sc)) {
            throw new SecurityException(ResultCode.UNAUTHORIZED);
        }
        SecurityContextHolder.setContext(sc);

        try {
            // 4. 兼容 Sa-Token 自身的注解鉴权（@SaCheckLogin、@SaCheckRole 等）
            if (isAnnotation) {
                SaAnnotationStrategy.instance.checkMethodAnnotation.accept(method);
            }
        } catch (RuntimeException | Error t) {
            // preHandle 抛异常时 Spring MVC 不会回调本拦截器的 afterCompletion
            // （HandlerExecutionChain 仅在 preHandle 返回 true 后才推进 interceptorIndex），
            // 已写入的上下文会残留在容器线程上被后续请求复用，因此在此主动清理后再抛出。
            // try 的范围紧贴 setContext 之后：此前的 checkLogin/getContext 抛异常时无上下文需要清理。
            SecurityContextHolder.clear();
            throw t;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理上下文，防止内存泄漏和跨请求数据污染
        SecurityContextHolder.clear();
    }
}
