package com.snowdrift.framework.security.spring.filter;

import com.snowdrift.framework.common.util.DesensitizeUtil;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.properties.SecurityProperties;
import com.snowdrift.framework.security.spring.util.SpringSecurityHelper;
import com.snowdrift.framework.security.store.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全上下文桥接过滤器
 * <p>
 * 在 Spring Security 过滤器链之后执行，从请求头提取 Token，
 * 通过 {@link TokenStore} 查找对应的 {@link SecurityContext}，
 * 将其写入：
 * <ul>
 *   <li>{@link SecurityContextHolder} —— 供业务代码获取用户信息</li>
 *   <li>Spring Security 的 {@code SecurityContextHolder} —— 确保 {@code @PreAuthorize} 生效</li>
 * </ul>
 * </p>
 *
 * @author 83674
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class SecurityContextFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final TokenStore tokenStore;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public SecurityContextFilter(SecurityProperties securityProperties, TokenStore tokenStore) {
        this.securityProperties = securityProperties;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 排除无需认证的路径（静态资源、文档、错误页等）
            String path = request.getServletPath();
            if (isExcludedPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 从请求头提取 Token
            String token = extractToken(request);
            if (StringUtils.isBlank(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 从 TokenStore 查找 SecurityContext
            SecurityContext sc = tokenStore.get(token);
            if (sc == null) {
                log.debug("Token 无效或已过期: token={}", DesensitizeUtil.process(token, "(?<=.{8}).+", "***"));
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 写入框架的 SecurityContextHolder（供业务代码通过 SecurityContextHolder.getContext() 获取）
            SecurityContextHolder.setContext(sc);

            // 5. 构建 Spring Security 的 Authentication（供 @PreAuthorize 等注解鉴权使用）
            SpringSecurityHelper.setSpringSecurityAuthentication(sc);
        } catch (Exception e) {
            log.error("SecurityContextFilter 执行异常: {}", e.getMessage(), e);
            throw new SecurityException(e);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理，防止线程池复用时的数据污染
            SecurityContextHolder.clear();
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    /**
     * 判断当前请求路径是否需要跳过认证
     */
    private boolean isExcludedPath(String path) {
        if (CollectionUtils.isEmpty(securityProperties.getExcludePathPatterns())) {
            return false;
        }
        for (String pattern : securityProperties.getExcludePathPatterns()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头提取 Token
     * 格式：Authorization: Bearer <token>
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(securityProperties.getHeaderName());
        if (org.apache.commons.lang3.StringUtils.isBlank(header)) {
            return null;
        }
        String prefix = securityProperties.getPrefix();
        if (StringUtils.isNotBlank(prefix) && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        // 无前缀时直接返回
        return header.trim();
    }

}
