package com.microservice.kotlin.config

import com.microservice.jwt.common.TokenProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.server.ResponseStatusException
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SpringSecurityConfiguration(@Autowired val tokenProvider: TokenProvider,
                                  @Autowired val customDefaultErrorAttributes: CustomDefaultErrorAttributes
                                  ) : WebSecurityConfigurerAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val WHITELIST = arrayOf(
        // -- swagger ui
        "/v2/api-docs", "/swagger-resources", "/swagger-resources/**", "/configuration/ui", "/configuration/security", "/swagger-ui.html", "/webjars/**", "/**/*.js", "/**/*.css", "/**/*.html", "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/**",
        "/error")

    override fun configure(http: HttpSecurity) {
        http
            .csrf()
            .disable()
            .headers()
            .frameOptions().disable()
            .cacheControl().disable()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .authorizeRequests()
            .antMatchers(*WHITELIST).permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterAt(authenticationJwtFilter(), UsernamePasswordAuthenticationFilter::class.java)
    }

    fun authenticationJwtFilter(): GenericFilterBean {
        return object: GenericFilterBean() {
            override fun doFilter(request: ServletRequest, response: ServletResponse, filter: FilterChain) {
                var authorizationHeader = (request as HttpServletRequest).getHeader(HttpHeaders.AUTHORIZATION)
                if (authorizationHeader.isNotBlank()) {
                    authorizationHeader = authorizationHeader.replaceFirst("Bearer ", "")
                }
                log.debug("Authorization header: {}", authorizationHeader)
                if (tokenProvider.validateToken(authorizationHeader).not()) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT")
                }
                val authentication = tokenProvider.getAuthentication(authorizationHeader)
                if (authentication != null) {
                    SecurityContextHolder.getContext().authentication = authentication
                }
                filter.doFilter(request, response)
            }
        }
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(*WHITELIST)
    }
}
