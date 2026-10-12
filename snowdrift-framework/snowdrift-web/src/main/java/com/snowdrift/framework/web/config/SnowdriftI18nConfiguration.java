package com.snowdrift.framework.web.config;

import com.snowdrift.framework.web.interceptor.I18nInterceptor;
import com.snowdrift.framework.web.properties.I18nProperties;
import com.snowdrift.framework.web.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * I18nConfiguration
 *
 * @author gaoyzelov
 * @date 2026/5/9
 * @description i18n国际化配置类
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
@ConditionalOnProperty(prefix = "snowdrift.i18n", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(I18nProperties.class)
public class SnowdriftI18nConfiguration implements WebMvcConfigurer {

    private final I18nProperties properties;
    private static final String CLASSPATH_PREFIX = "classpath:";

    public SnowdriftI18nConfiguration(I18nProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 MessageSource
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames(properties.getBaseNames().stream()
                .map(name -> name.startsWith(CLASSPATH_PREFIX) ? name : CLASSPATH_PREFIX + name)
                .toArray(String[]::new));
        messageSource.setDefaultEncoding(properties.getEncoding());
        messageSource.setUseCodeAsDefaultMessage(properties.getUseCodeAsDefaultMessage());
        // 缓存配置：-1 表示永不缓存（开发环境），正数表示缓存秒数（生产环境）
        messageSource.setCacheSeconds(properties.getCacheSeconds());
        messageSource.setDefaultLocale(I18nUtil.parseLocale(properties.getDefaultLocale()));
        return messageSource;
    }

    /**
     * 把「最终生效的 MessageSource」绑定到 {@link I18nUtil}
     * <p>
     * {@link #messageSource()} 带 {@code @ConditionalOnMissingBean}：宿主（或 Boot 的
     * {@code MessageSourceAutoConfiguration}）已经提供 MessageSource 时，本方法不执行，
     * 而 {@code I18nUtil} 的静态字段此前<b>只在该方法里</b>赋值 —— 于是表现为「i18n 明明配了、
     * 却总是回落到 code」且只在运行期打一条「消息源未初始化」的 WARN。
     * </p>
     * <p>
     * 这里改为在容器启动完成后按类型取「容器里生效的那个」，无论它由谁提供，都绑定一次。
     * 用 {@code getIfUnique()} 而非 {@code getIfAvailable()}：存在多个 MessageSource 属配置错误，
     * 此时只告警、不阻断启动（原来也不会因此失败）。
     * </p>
     */
    @Bean
    public SmartInitializingSingleton i18nUtilMessageSourceBinder(ObjectProvider<MessageSource> messageSources) {
        return () -> {
            MessageSource source = messageSources.getIfUnique();
            if (source == null) {
                log.warn("[Snowdrift-Web] i18n 已启用，但容器中不存在唯一的 MessageSource"
                        + "（缺失或存在多个候选），I18nUtil 的消息查找将回落到 code");
                return;
            }
            I18nUtil.initMessageSource(source);
            log.info("[Snowdrift-Web] I18nUtil 已绑定 MessageSource: {}", source.getClass().getName());
        };
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new I18nInterceptor(properties));
    }
}
