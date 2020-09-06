package com.microservice.web.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Slf4j
@Configuration
@EnableWebFlux
public class WebConfiguration implements WebFluxConfigurer {
    @Autowired
    private Environment environment;

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

    @Bean
    CorsWebFilter corsWebFilter() {
        log.debug("active profiles: {}", environment.getActiveProfiles());
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            corsConfig.addAllowedOrigin("https://spendingbetter.com");
        } else {
            corsConfig.addAllowedOrigin("*");
        }
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
