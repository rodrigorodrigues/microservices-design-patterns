package com.microservice.kotlin.config

import com.microservice.jwt.common.TokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
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
            .addFilterAt(authenticationJwtFilter(), UsernamePasswordAuthenticationFilter::class.java)
/*
            .exceptionHandling()
            .authenticationEntryPoint({ exchange, e -> handleResponseError.handle(exchange, e, true) })
            .and()
*/
    }

    fun authenticationJwtFilter(): GenericFilterBean {
        return object: GenericFilterBean() {
            override fun doFilter(request: ServletRequest, response: ServletResponse, filter: FilterChain) {
                val authorizationHeader = (request as HttpServletRequest).getHeader(HttpHeaders.AUTHORIZATION)
                if (!tokenProvider.validateToken(authorizationHeader)) {
                    throw UnauthorizedUserException("Invalid JWT")
                }
                filter.doFilter(request, response)
            }
        }
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(*WHITELIST)
    }
}
