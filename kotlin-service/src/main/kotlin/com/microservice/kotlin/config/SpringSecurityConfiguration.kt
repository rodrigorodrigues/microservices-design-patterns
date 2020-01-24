package com.microservice.kotlin.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SpringSecurityConfiguration(@Autowired val tokenStore: TokenStore) : WebSecurityConfigurerAdapter() {
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

                if (authorizationHeader.isBlank()) {
                    authorizationHeader = request.getHeader("authorization")
                }

                if (authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ", true)) {
                    unauthorized(response as HttpServletResponse)
                    return
                }
                authorizationHeader = authorizationHeader.replaceFirst("bearer ", "", true)
                log.debug("Authorization header: {}", authorizationHeader)
                val oAuth2Authentication = tokenStore.readAuthentication(authorizationHeader)
                if (oAuth2Authentication == null) {
                    unauthorized(response as HttpServletResponse)
                    return
                }
                val authentication = oAuth2Authentication.userAuthentication
                if (authentication != null) {
                    SecurityContextHolder.getContext().authentication = authentication
                }
                filter.doFilter(request, response)
            }
        }
    }

    private fun unauthorized(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT")
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().antMatchers(*WHITELIST)
    }
}
