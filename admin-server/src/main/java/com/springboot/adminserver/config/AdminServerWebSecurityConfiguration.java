package com.springboot.adminserver.config;

import com.microservice.authentication.common.service.SharedAuthenticationService;
import lombok.AllArgsConstructor;
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
public class AdminServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final String[] WHITELIST = {
        "/actuator/**",
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico"
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
