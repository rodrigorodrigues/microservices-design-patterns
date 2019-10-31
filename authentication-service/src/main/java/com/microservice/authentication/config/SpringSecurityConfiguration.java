package com.microservice.authentication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.web.util.CustomDefaultErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import java.util.Map;

/**
 * Spring Security Configuration
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter implements ApplicationContextAware {
    private final SharedAuthenticationServiceImpl sharedAuthenticationService;

    private final ObjectMapper objectMapper;

    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    private ApplicationContext applicationContext;

    public SpringSecurityConfiguration(SharedAuthenticationServiceImpl sharedAuthenticationService, ObjectMapper objectMapper, CustomDefaultErrorAttributes customDefaultErrorAttributes) {
        this.sharedAuthenticationService = sharedAuthenticationService;
        this.objectMapper = objectMapper;
        this.customDefaultErrorAttributes = customDefaultErrorAttributes;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        super.setApplicationContext(context);
        this.applicationContext = context;
    }

    private static final String[] WHITELIST = {
        // -- swagger ui
        "/v2/api-docs",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/**"
    };

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
            http.requestMatchers()
                .antMatchers("/login", "/logout")
                .and()
                .csrf().disable()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .formLogin()
                .successHandler(successHandler())
                .failureHandler(authenticationFailureHandler())
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
            .and()
            .logout()
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, exception,true);
            response.setStatus(Integer.parseInt(errorAttributes.get("status").toString()));
            response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
        };
    }

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            JwtAccessTokenConverter jwtAccessTokenConverter = applicationContext.getBean(JwtAccessTokenConverter.class);
            String authorization = "Bearer "; //+ tokenProvider.createToken(authentication, authentication.getName(), false);
            JwtTokenDto jwtToken = new JwtTokenDto(authorization);
            response.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("sessionId", request.getSession().getId());
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().append(objectMapper.writeValueAsString(jwtToken));
        };
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(WHITELIST);
    }
}
