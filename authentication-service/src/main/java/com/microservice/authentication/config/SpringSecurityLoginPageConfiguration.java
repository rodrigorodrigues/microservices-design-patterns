package com.microservice.authentication.config;

import com.microservice.authentication.service.CustomAuthenticationSuccessHandler;
import com.microservice.authentication.service.CustomOidcUserService;
import lombok.AllArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Order(303)
@Profile("!prod")
public class SpringSecurityLoginPageConfiguration {
    private final CustomOidcUserService customOidcUserService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final LogoutSuccessHandler customLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
        throws Exception {
        return http
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(SpringSecurityFormConfiguration.WHITELIST).permitAll()
                .anyRequest().authenticated()
            )
            // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .oauth2Login(o -> o.successHandler(customAuthenticationSuccessHandler)
                .userInfoEndpoint(u -> u.oidcUserService(customOidcUserService)))
            .formLogin(Customizer.withDefaults())
            .logout(l -> l.logoutSuccessUrl("/logout")
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.GET.name()))
                .invalidateHttpSession(true))
            .build();
    }

}
