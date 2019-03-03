package com.springboot.eurekaserver.config;

import com.learning.springboot.service.SharedAuthenticationServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class EurekaServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final SharedAuthenticationServiceImpl sharedAuthenticationService;

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/eureka/apps/**", "/logout",
                "/actuator/**", "/v1/agent/self",
                "/v1/catalog/services", "/v1/catalog/service/**").permitAll()
            .anyRequest().hasRole("ADMIN")
            .and()
            .formLogin()
            .and()
            .logout();
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
