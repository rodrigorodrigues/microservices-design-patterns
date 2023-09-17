package com.springboot.adminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class AdminServerWebSecurityConfiguration {
    private static final String[] WHITELIST = {
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/info",
        "/actuator/health/**",
        "/actuator/prometheus",
        "/error"
    };

    private final AdminServerProperties adminServerProperties;

    private final TokenStore tokenStore;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String adminContextPath = adminServerProperties.getContextPath();

        return http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a -> a.requestMatchers(WHITELIST).permitAll()
                .requestMatchers(adminContextPath + "/assets/**").permitAll()
                .requestMatchers(adminContextPath + "/login").permitAll()
                .requestMatchers(adminContextPath + "/logout").permitAll()
                .requestMatchers(adminContextPath + "/error").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(f -> f.loginPage(adminContextPath + "/login"))
            .logout(l -> l.logoutUrl(adminContextPath + "/logout").deleteCookies("SESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    log.info("Logout success!");
                    if (authentication instanceof OAuth2AuthenticationToken) {
                        tokenStore.findTokensByClientId(authentication.getName())
                            .forEach(tokenStore::removeAccessToken);
                    }
                    new SimpleUrlLogoutSuccessHandler();
                })
                .invalidateHttpSession(true))
            .build();
    }
}
