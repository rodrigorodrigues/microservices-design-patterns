package com.springboot.configserver.config;

import com.learning.springboot.service.SharedAuthenticationServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class ConfigServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final SharedAuthenticationServiceImpl sharedAuthenticationService;

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/actuator/**").permitAll()
            .anyRequest().access("@webSecurity.checkEncryptKey(request, authentication)")
            .and()
            .formLogin()
            .and()
            .logout().permitAll();
    }
}
