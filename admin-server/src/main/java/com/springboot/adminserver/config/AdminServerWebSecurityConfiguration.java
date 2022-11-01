package com.springboot.adminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class AdminServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String adminContextPath = adminServerProperties.getContextPath();

        http.csrf().disable()
            .authorizeRequests()
            .antMatchers(WHITELIST).permitAll()
            .antMatchers(adminContextPath + "/assets/**").permitAll()
            .antMatchers(adminContextPath + "/login").permitAll()
            .antMatchers(adminContextPath + "/logout").permitAll()
            .antMatchers(adminContextPath + "/error").permitAll()
            .anyRequest().hasRole("ADMIN")
            .and()
            .formLogin()
                .loginPage(adminContextPath + "/login")
            .and()
            .logout()
                .logoutUrl(adminContextPath + "/logout")
            .deleteCookies("SESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> {
                log.info("Logout success!");
                if (authentication instanceof OAuth2AuthenticationToken) {
                    tokenStore.findTokensByClientId(authentication.getName())
                        .forEach(tokenStore::removeAccessToken);
                }
                new SimpleUrlLogoutSuccessHandler();
            })
            .invalidateHttpSession(true);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(WHITELIST);
    }
}
