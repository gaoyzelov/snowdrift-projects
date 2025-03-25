package com.snowdrift.web.config;

import com.snowdrift.web.aspect.AccessLogAspect;
import com.snowdrift.web.handler.IAccessLogHandler;
import com.snowdrift.web.handler.DefaultAccessLogHandler;
import com.snowdrift.web.properties.CorsProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

/**
 * WebAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/24 14:23:09
 * @description web自动配置
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(AccessLogAspect.class)
public class WebAutoConfiguration implements WebMvcConfigurer {

    @Resource
    private CorsProperties corsProperties;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<CorsProperties.CorsItem> corsItems = corsProperties.getCorsItems();
        if (CollectionUtils.isEmpty(corsItems)) {
            return;
        }
        for (CorsProperties.CorsItem corsItem : corsItems) {
            registry.addMapping(corsItem.getPathPattern())
                    .allowedOriginPatterns(corsItem.getAllowedOriginPatterns())
                    .allowedMethods(corsItem.getAllowedMethods())
                    .allowedHeaders(corsItem.getAllowedHeaders())
                    .allowCredentials(corsItem.getAllowCredentials())
                    .maxAge(corsItem.getMaxAge());
        }
    }

    /**
     * 默认的访问日志处理器
     *
     * @return 访问日志处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public IAccessLogHandler defaultAccessLogHandler() {
        return new DefaultAccessLogHandler();
    }
}