package com.snowdrift.framework.security.satoken.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.listener.SaTokenListener;
import com.snowdrift.framework.security.satoken.handler.SaTokenExceptionHandler;
import com.snowdrift.framework.security.satoken.listener.TokenStateListener;
import com.snowdrift.framework.security.satoken.properties.SaTokenSecurityProperties;
import com.snowdrift.framework.security.satoken.service.SaTokenSecurityServiceImpl;
import com.snowdrift.framework.security.service.ISecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftSecuritySaTokenConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Sa-Token 自动配置黑盒测试：属性→SaTokenConfig 映射、Banner 关闭、各 Bean 工厂返回正确类型
 * @since 1.0.0
 */
class SnowdriftSecuritySaTokenConfigurationTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ISecurityService> provider = mock(ObjectProvider.class);

    private SnowdriftSecuritySaTokenConfiguration config(SaTokenSecurityProperties props) {
        return new SnowdriftSecuritySaTokenConfiguration(props, provider);
    }

    @Test
    @DisplayName("saTokenConfig：将自定义属性完整映射到 SaTokenConfig，并关闭 Banner")
    void mapsPropertiesToSaTokenConfig() {
        SaTokenSecurityProperties props = new SaTokenSecurityProperties();
        props.setHeaderName("X-Token");
        props.setTimeout(100L);
        props.setActiveTimeout(50L);
        props.setPrefix("TK");
        props.setConcurrent(true);
        props.setIsShare(true);
        props.setMaxLoginCount(5);
        props.setTokenStyle("tik");
        props.setIsLog(true);

        SaTokenConfig config = config(props).saTokenConfig();

        assertThat(config.getTokenName()).isEqualTo("X-Token");
        assertThat(config.getTimeout()).isEqualTo(100L);
        assertThat(config.getActiveTimeout()).isEqualTo(50L);
        assertThat(config.getTokenPrefix()).isEqualTo("TK");
        assertThat(config.getIsConcurrent()).isTrue();
        assertThat(config.getIsShare()).isTrue();
        assertThat(config.getMaxLoginCount()).isEqualTo(5);
        assertThat(config.getTokenStyle()).isEqualTo("tik");
        assertThat(config.getIsLog()).isTrue();
        // 框架强制关闭 Banner 打印
        assertThat(config.getIsPrint()).isFalse();
    }

    @Test
    @DisplayName("securityService：注册 Sa-Token 实现")
    void registersSecurityService() {
        assertThat(config(new SaTokenSecurityProperties()).securityService())
                .isInstanceOf(SaTokenSecurityServiceImpl.class)
                .isInstanceOf(ISecurityService.class);
    }

    @Test
    @DisplayName("saTokenExceptionHandler / saTokenListener：注册对应 Bean")
    void registersHandlerAndListener() {
        SnowdriftSecuritySaTokenConfiguration configuration = config(new SaTokenSecurityProperties());

        assertThat(configuration.saTokenExceptionHandler()).isInstanceOf(SaTokenExceptionHandler.class);
        SaTokenListener listener = configuration.saTokenListener();
        assertThat(listener).isInstanceOf(TokenStateListener.class);
    }
}
