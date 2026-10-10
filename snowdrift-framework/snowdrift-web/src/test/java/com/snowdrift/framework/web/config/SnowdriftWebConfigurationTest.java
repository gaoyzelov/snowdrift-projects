package com.snowdrift.framework.web.config;

import com.snowdrift.framework.web.handler.WebExceptionHandler;
import com.snowdrift.framework.web.properties.CorsProperties;
import com.snowdrift.framework.web.properties.ResourceProperties;
import com.snowdrift.framework.web.properties.XssProperties;
import com.snowdrift.framework.web.xss.SimpleXssCleaner;
import com.snowdrift.framework.web.xss.XssJsonBodyAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.core.Ordered;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SnowdriftWebConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Web 自动配置黑盒测试：CORS/静态资源开关与映射、日期时间格式化注册、各 Filter/Advice Bean 与过滤器链路顺序
 * @since 1.0.0
 */
class SnowdriftWebConfigurationTest {

    private SnowdriftWebConfiguration config(ResourceProperties resource, CorsProperties cors, XssProperties xss) {
        return new SnowdriftWebConfiguration(resource, cors, xss);
    }

    // ========== CORS ==========

    @Test
    @DisplayName("CORS 启用：按配置注册映射与凭证/方法/缓存")
    void corsEnabledRegistersMapping() {
        CorsProperties cors = new CorsProperties();
        cors.setEnabled(true);
        cors.setPath("/api/**");
        cors.setAllowedOriginPatterns(List.of("http://localhost:*"));
        cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(1800L);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class, Answers.RETURNS_SELF);
        when(registry.addMapping("/api/**")).thenReturn(registration);

        config(new ResourceProperties(), cors, new XssProperties()).addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registration).allowedOriginPatterns("http://localhost:*");
        verify(registration).allowedMethods("GET", "POST");
        verify(registration).allowCredentials(true);
        verify(registration).maxAge(1800L);
    }

    @Test
    @DisplayName("CORS 关闭：不注册任何映射")
    void corsDisabledNoMapping() {
        CorsProperties cors = new CorsProperties();
        cors.setEnabled(false);
        CorsRegistry registry = mock(CorsRegistry.class);

        config(new ResourceProperties(), cors, new XssProperties()).addCorsMappings(registry);

        verify(registry, never()).addMapping(any());
    }

    // ========== 静态资源 ==========

    @Test
    @DisplayName("静态资源启用：注册映射，useCacheControl=true 时用配置的 cachePeriod")
    void resourceEnabledUsesCachePeriod() {
        ResourceProperties resource = new ResourceProperties();
        resource.setEnabled(true);
        ResourceProperties.ResourceMapping mapping = new ResourceProperties.ResourceMapping();
        mapping.setPathPattern("/files/**");
        mapping.setLocation("file:/tmp/");
        mapping.setCachePeriod(3600);
        mapping.setUseCacheControl(true);
        resource.setMappings(List.of(mapping));

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class, Answers.RETURNS_SELF);
        when(registry.addResourceHandler("/files/**")).thenReturn(registration);

        config(resource, new CorsProperties(), new XssProperties()).addResourceHandlers(registry);

        verify(registry).addResourceHandler("/files/**");
        verify(registration).addResourceLocations("file:/tmp/");
        verify(registration).setCachePeriod(3600);
    }

    @Test
    @DisplayName("静态资源：useCacheControl=false 时 cachePeriod 置 0（不缓存）")
    void resourceDisabledCacheControlUsesZero() {
        ResourceProperties resource = new ResourceProperties();
        resource.setEnabled(true);
        ResourceProperties.ResourceMapping mapping = new ResourceProperties.ResourceMapping();
        mapping.setPathPattern("/files/**");
        mapping.setLocation("file:/tmp/");
        mapping.setCachePeriod(3600);
        mapping.setUseCacheControl(false);
        resource.setMappings(List.of(mapping));

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class, Answers.RETURNS_SELF);
        when(registry.addResourceHandler("/files/**")).thenReturn(registration);

        config(resource, new CorsProperties(), new XssProperties()).addResourceHandlers(registry);

        verify(registration).setCachePeriod(0);
    }

    @Test
    @DisplayName("静态资源关闭或无映射：不注册")
    void resourceDisabledNoHandler() {
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);

        ResourceProperties disabled = new ResourceProperties();
        disabled.setEnabled(false);
        config(disabled, new CorsProperties(), new XssProperties()).addResourceHandlers(registry);

        ResourceProperties empty = new ResourceProperties();
        empty.setEnabled(true);
        empty.setMappings(List.of());
        config(empty, new CorsProperties(), new XssProperties()).addResourceHandlers(registry);

        verify(registry, never()).addResourceHandler(any());
    }

    // ========== 日期时间格式化 ==========

    @Test
    @DisplayName("addFormatters 注册日期/时间/日期时间格式化器（无默认格式化器时仍可解析约定格式）")
    void addFormattersRegistersDateTimeFormatters() {
        DefaultFormattingConversionService registry = new DefaultFormattingConversionService(false);

        config(new ResourceProperties(), new CorsProperties(), new XssProperties()).addFormatters(registry);

        assertThat(registry.convert("2026-01-02", LocalDate.class)).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(registry.convert("03:04:05", LocalTime.class)).isEqualTo(LocalTime.of(3, 4, 5));
        assertThat(registry.convert("2026-01-02 03:04:05", LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
    }

    // ========== Bean 与过滤器顺序 ==========

    @Test
    @DisplayName("Bean 工厂方法返回正确类型")
    void beanFactories() {
        SnowdriftWebConfiguration configuration =
                config(new ResourceProperties(), new CorsProperties(), new XssProperties());

        assertThat(configuration.webExceptionHandler()).isInstanceOf(WebExceptionHandler.class);
        assertThat(configuration.xssCleaner()).isInstanceOf(SimpleXssCleaner.class);
        assertThat(configuration.xssJsonBodyAdvice(new SimpleXssCleaner())).isInstanceOf(XssJsonBodyAdvice.class);
    }

    @Test
    @DisplayName("过滤器注册顺序符合 README 链路（CachedBody < HttpContext < LogTrace < Xss）")
    void filterOrderMatchesDocumentedChain() {
        SnowdriftWebConfiguration configuration =
                config(new ResourceProperties(), new CorsProperties(), new XssProperties());

        assertThat(configuration.cachedBodyFilter().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(configuration.httpContextFilter().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
        assertThat(configuration.logTraceFilter().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 5);
        assertThat(configuration.xssFilter(new SimpleXssCleaner()).getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
    }
}
