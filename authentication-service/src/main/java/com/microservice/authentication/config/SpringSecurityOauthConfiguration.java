package com.microservice.authentication.config;

import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security Configuration for ouath2
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Order(1)
public class SpringSecurityOauthConfiguration extends WebSecurityConfigurerAdapter {

    private final ServerProperties serverProperties;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String contextPath = (serverProperties.getServlet() != null && StringUtils.isNotBlank(serverProperties.getServlet().getContextPath()) ? serverProperties.getServlet().getContextPath() : "");
        http.requestMatchers()
            .and()
            .requestMatchers()
            .antMatchers(contextPath + "/login", contextPath + "/oauth/authorize")
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .permitAll()
            .and()
            .logout()
            .deleteCookies("SESSIONID")
            .invalidateHttpSession(true)
            .and()
            .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringAntMatchers("/oauth/**");
    }

}
