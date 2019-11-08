package com.microservice.kotlin.config

import com.microservice.jwt.common.TokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SpringSecurityConfiguration(@Autowired val tokenProvider: TokenProvider
//                                  @Autowired val handleResponseError: HandleResponseError
                                  ) : WebSecurityConfigurerAdapter() {

    private val WHITELIST = arrayOf(
        // -- swagger ui
        "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui.html", "/webjars/**", "/**/*.js", "/**/*.css", "/**/*.html", "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/**")

    override fun configure(http: HttpSecurity) {
        http
            .csrf()
            .disable()
            .headers()
            .frameOptions().disable()
            .cacheControl().disable()
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .authorizeRequests()
            .antMatchers(*WHITELIST).permitAll()
            .anyRequest().authenticated()
            .and()
            //.addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
/*
            .exceptionHandling()
            .authenticationEntryPoint({ exchange, e -> handleResponseError.handle(exchange, e, true) })
            .and()
*/
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(*WHITELIST)
    }
}
