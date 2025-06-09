package com.snowdrift.doc.knife4j.config;

import com.snowdrift.doc.knife4j.properties.Knife4jApiDocProperties;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4jApiDocAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/18 17:42:22
 * @description Knife4j API接口文档配置
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(Knife4jApiDocProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "snowdrift.doc.knife4j", name = "enabled", havingValue = "true")
public class Knife4jApiDocAutoConfiguration {

    @Bean
    public GroupedOpenApi groupedOpenApi(Knife4jApiDocProperties prop) {
        return GroupedOpenApi.builder()
                .group(prop.getGroup())
                .displayName(prop.getDisplayName())
                .packagesToScan(prop.getBasePackages().toArray(new String[0]))
                .addOpenApiCustomizer(openApi -> openApi
                        .setInfo((new Info()
                        .title(prop.getTitle())
                        .version(prop.getVersion())
                        .description(prop.getDescription())
                        .termsOfService(prop.getWebsite())
                        .license(new License().name("Apache 2.0").url("http://doc.xiaominfo.com")))))
                .build();
    }
}