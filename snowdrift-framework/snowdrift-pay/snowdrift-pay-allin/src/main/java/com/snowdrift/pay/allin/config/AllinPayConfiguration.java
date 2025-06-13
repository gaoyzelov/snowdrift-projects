package com.snowdrift.pay.allin.config;


import com.snowdrift.pay.allin.properties.AllinBrandProperties;
import com.snowdrift.pay.allin.properties.AllinSybProperties;
import com.snowdrift.pay.allin.service.IAllinBrandService;
import com.snowdrift.pay.allin.service.IAllinSybService;
import com.snowdrift.pay.allin.service.impl.AllinBrandServiceImpl;
import com.snowdrift.pay.allin.service.impl.AllinSybServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AllinPayConfiguration
 *
 * @author gaoye
 * @date 2025/05/20 19:45:32
 * @description xxxxxxxx
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties({AllinSybProperties.class, AllinBrandProperties.class})
public class AllinPayConfiguration {

    /**
     * 收银宝接口服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pay.allin.syb", name = "enabled", havingValue = "true")
    public IAllinSybService allinSybService(AllinSybProperties prop) {
        return new AllinSybServiceImpl(prop);
    }

    /**
     * 智品牌接口服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pay.allin.brand", name = "enabled", havingValue = "true")
    public IAllinBrandService allinBrandService(AllinBrandProperties prop) {
        return new AllinBrandServiceImpl(prop);
    }
}