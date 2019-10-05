package com.microservice.web.autoconfigure;

import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import com.microservice.web.common.util.CustomDefaultErrorAttributes;
import com.microservice.web.common.util.GlobalExceptionHandler;
import com.microservice.web.common.util.HandleResponseError;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

@Configuration
@EnableConfigurationProperties(Java8SpringConfigurationProperties.class)
public class WebCommonAutoConfiguration {
    @Primary
    @Bean
    CustomDefaultErrorAttributes customDefaultErrorAttributes(HandleResponseError handleResponseError) {
        return new CustomDefaultErrorAttributes(handleResponseError);
    }

    @Bean
    HandleResponseError handleResponseError() {
        return new HandleResponseError();
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                                                  ServerProperties serverProperties, ApplicationContext applicationContext,
                                                  HandleResponseError handleResponseError, ObjectProvider<ViewResolver> viewResolversProvider,
                                                  ServerCodecConfigurer serverCodecConfigurer) {
        return new GlobalExceptionHandler(errorAttributes, resourceProperties, serverProperties, applicationContext, handleResponseError, viewResolversProvider, serverCodecConfigurer);
    }
}
