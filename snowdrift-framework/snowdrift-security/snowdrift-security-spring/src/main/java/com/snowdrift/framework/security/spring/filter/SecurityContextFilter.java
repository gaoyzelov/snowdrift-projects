package com.snowdrift.framework.security.spring.filter;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.security.properties.SecurityProperties;
import com.snowdrift.framework.security.spring.store.TokenStore;
import com.snowdrift.framework.security.spring.util.SpringSecurityHelper;
import com.snowdrift.framework.security.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

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
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class SecurityContextFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final TokenStore tokenStore;

    /**
     * 排除路径的匹配模式
     * <p>与安全链保持同一套语义：链上 {@code requestMatchers(..)} 由 {@code MvcRequestMatcher} 交给
     * Spring MVC 的 PatternParser 求值，此处同样使用 {@link PathPatternParser}，避免同一份
     * {@code excludePathPatterns} 被两套规则解释（{@code AntPathMatcher} 与 {@code PathPattern}
     * 对 {@code **} 的位置、空路径段等的处理并不一致）。</p>
     * <p>构造时即解析，模式非法会在启动阶段直接暴露，而不是运行期静默不匹配。</p>
     */
    private final List<PathPattern> excludePatterns;

    public SecurityContextFilter(SecurityProperties securityProperties, TokenStore tokenStore) {
        this.securityProperties = securityProperties;
        this.tokenStore = tokenStore;
        PathPatternParser parser = new PathPatternParser();
        this.excludePatterns = CollectionUtils.emptyIfNull(securityProperties.getExcludePathPatterns())
                .stream()
                .map(parser::parse)
                .toList();
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
                log.debug("Token 无效或已过期: token={}", SecurityUtil.desensitizeToken(token));
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 写入框架的 SecurityContextHolder（供业务代码通过 SecurityContextHolder.getContext() 获取）
            SecurityContextHolder.setContext(sc);

            // 5. 构建 Spring Security 的 Authentication（供 @PreAuthorize 等注解鉴权使用）
            SpringSecurityHelper.setSpringSecurityAuthentication(sc);

            // 6. 继续执行后续的过滤器链
            filterChain.doFilter(request, response);
        } catch (Exception e){
            log.error("SecurityContextFilter 执行异常: {}", e.getMessage(), e);
            throw e;
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
        if (excludePatterns.isEmpty()) {
            return false;
        }
        PathContainer container = PathContainer.parsePath(path);
        for (PathPattern pattern : excludePatterns) {
            if (pattern.matches(container)) {
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
        if (StringUtils.isBlank(header)) {
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
