package com.springboot.eurekaserver.config;

import com.microservice.authentication.common.service.SharedAuthenticationService;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@EnableOAuth2Sso
public class EurekaServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String[] WHITELIST = {
        "/eureka/apps/**",
        "/v1/agent/self",
        "/eureka/peerreplication/batch/**",
        "/v1/catalog/services",
        "/v1/catalog/service/**",
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
    };

    private final SharedAuthenticationService sharedAuthenticationService;

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirect_uri");
        successHandler.setDefaultTargetUrl("/");

        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/login", "/logout").permitAll()
            .anyRequest().hasRole("ADMIN")
            .and()
            .formLogin().loginPage("/login").successHandler(successHandler)
            .and()
            .logout()
            .deleteCookies("SESSIONID")
            .invalidateHttpSession(true);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(WHITELIST);
    }
}
