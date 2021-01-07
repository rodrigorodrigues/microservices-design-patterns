package com.springboot.adminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@EnableOAuth2Sso
public class AdminServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final String[] WHITELIST = {
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/info",
        "/actuator/health",
        "/actuator/prometheus",
        "/error"
    };

    private final AdminServerProperties adminServerProperties;

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
            .logout();
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(WHITELIST);
    }
}
