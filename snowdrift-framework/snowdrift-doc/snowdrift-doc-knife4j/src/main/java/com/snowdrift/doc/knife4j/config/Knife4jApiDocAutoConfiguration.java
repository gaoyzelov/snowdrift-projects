package com.snowdrift.doc.knife4j.config;

import com.snowdrift.core.constant.StrConst;
import com.snowdrift.doc.knife4j.processor.ApiDocBeanPostProcessor;
import com.snowdrift.doc.knife4j.properties.Knife4jApiDocProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.Collections;
import java.util.List;

/**
 * Knife4jApiDocAutoConfiguration
 *
 * @author gaoye
 * @date 2025/03/18 17:42:22
 * @description Knife4j API接口文档配置
 * @since 1.0.0
 */
@Configuration
@EnableSwagger2WebMvc
@Import(ApiDocBeanPostProcessor.class)
@EnableConfigurationProperties(Knife4jApiDocProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "snowdrift.doc.knife4j", name = "enabled", havingValue = "true")
public class Knife4jApiDocAutoConfiguration {

    @Bean
    @Primary
    public Docket defaultApiDoc(Knife4jApiDocProperties properties) {
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(true)
                .host(properties.getWebsite())
                .apiInfo(apiInfo(properties))
                .select()
                .apis(RequestHandlerSelectors.basePackage(properties.getBasePackage()))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 添加默认API信息
     */
    private ApiInfo apiInfo(Knife4jApiDocProperties properties) {
        return new ApiInfoBuilder()
                .title(properties.getTitle())
                .description("# " + properties.getDescription())
                .termsOfServiceUrl(properties.getWebsite())
                .contact(properties.getContact())
                .version(properties.getVersion())
                .build();
    }

    /**
     * 添加默认请求头配置
     * docket.globalOperationParameters(globalHeaderParams());
     */
    private List<Parameter> globalHeaderParams() {
        ParameterBuilder builder = new ParameterBuilder()
                .name(StrConst.EMPTY)
                .description("令牌")
                .defaultValue(StrConst.EMPTY)
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(true);
        return Collections.singletonList(builder.build());
    }
}