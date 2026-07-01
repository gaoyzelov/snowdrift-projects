package com.snowdrift.framework.security.satoken.interceptor;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.annotation.AnonymousAccess;
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
 *   <li>{@link AnonymousAccess} 注解跳过认证</li>
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
        if (handler instanceof HandlerMethod handlerMethod) {
            Method method = handlerMethod.getMethod();

            // 1. 标记了 @AnonymousAccess 的方法直接放行
            if (method.isAnnotationPresent(AnonymousAccess.class)) {
                return true;
            }

            // 2. 兼容 Sa-Token 自身的注解鉴权（@SaCheckLogin、@SaCheckRole 等）
            if (isAnnotation) {
                SaAnnotationStrategy.instance.checkMethodAnnotation.accept(method);
            }

            // 3. 调用安全服务校验登录状态
            securityService.checkLogin();

            // 4. 从会话中捞出 SecurityContext 并放入 ThreadLocal，供后续业务代码使用
            SecurityContext sc = securityService.getContext();
            if (Objects.isNull(sc)) {
                throw new SecurityException("security.context.lost");
            }
            SecurityContextHolder.setContext(sc);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理上下文，防止内存泄漏和跨请求数据污染
        SecurityContextHolder.clear();
    }
}
