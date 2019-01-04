package com.learning.springboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfiguration implements WebFluxConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui.html**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/v2/api-docs")
                .addResourceLocations("classpath:/META-INF/v2/api-docs");

        registry.addResourceHandler("/swagger-resources")
                .addResourceLocations("classpath:/META-INF/swagger-resources");

        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/swagger-resources/**");

        registry.addResourceHandler("/configuration/ui")
                .addResourceLocations("classpath:/META-INF/configuration/ui");

        registry.addResourceHandler("/configuration/security")
                .addResourceLocations("classpath:/META-INF/configuration/security");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
