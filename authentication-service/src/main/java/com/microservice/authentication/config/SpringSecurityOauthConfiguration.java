package com.microservice.authentication.config;

import com.microservice.authentication.service.CustomAuthenticationSuccessHandler;
import com.microservice.authentication.service.CustomLogoutSuccessHandler;
import com.microservice.authentication.service.CustomOidcUserService;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security Configuration for oauth2
 */
@ConditionalOnProperty(prefix = "oauth", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Order(302)
public class SpringSecurityOauthConfiguration extends WebSecurityConfigurerAdapter {

    private final CustomOidcUserService customOidcUserService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers()
            .antMatchers("/oauth2/**", "/logout", "/login/**")
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and().logout()
                .logoutSuccessUrl("/logout")
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.GET.name()))
                .invalidateHttpSession(true)
            .and().oauth2Login()
                .successHandler(customAuthenticationSuccessHandler)
                .userInfoEndpoint()
                .oidcUserService(customOidcUserService);
    }

}
