package com.snowdrift.pay.yee.config;

import com.snowdrift.pay.yee.properties.YeeProperties;
import com.snowdrift.pay.yee.provider.CredentialsProvider;
import com.snowdrift.pay.yee.provider.SdkConfigProvider;
import com.snowdrift.pay.yee.service.IYeePayService;
import com.snowdrift.pay.yee.service.impl.YeePayServiceImpl;
import com.yeepay.yop.sdk.auth.credentials.provider.YopCredentialsProviderRegistry;
import com.yeepay.yop.sdk.base.config.provider.YopSdkConfigProviderRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * YeePayAutoConfiguration
 *
 * @author gaoye
 * @date 2025/06/10 16:08:51
 * @description xxxxxxxx
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(YeeProperties.class)
@ConditionalOnProperty(prefix = "pay.yee", name = "enabled", havingValue = "true")
public class YeePayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IYeePayService yeePayService(YeeProperties prop){
        CredentialsProvider credentialsProvider = new CredentialsProvider(prop);
        SdkConfigProvider sdkConfigProvider = new SdkConfigProvider(prop);
        YopCredentialsProviderRegistry.registerProvider(credentialsProvider);
        YopSdkConfigProviderRegistry.registerProvider(sdkConfigProvider);
        return new YeePayServiceImpl(prop);
    }
}