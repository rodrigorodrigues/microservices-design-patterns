package com.microservice.kotlin.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.web.common.util.CustomDefaultErrorAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.context.request.ServletWebRequest
import java.io.IOException

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
class SpringSecurityConfiguration(@Autowired val customDefaultErrorAttributes: CustomDefaultErrorAttributes,
                                  @Autowired val objectMapper: ObjectMapper,
                                  @Autowired val jwtDecoder: JwtDecoder,
                                  @Autowired val properties: AuthenticationProperties) {
    private val WHITE_LIST = arrayOf(
        // -- swagger ui
        // -- swagger ui
        "/v3/api-docs/**",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        "/*.js",
        "/*.css",
        "/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/**",
        "/error")

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf{ it.disable() }
            .headers { it -> it.frameOptions { it.disable() }.cacheControl { it.disable() } }
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests { it.requestMatchers(*WHITE_LIST).permitAll()
                .anyRequest().authenticated() }
            .oauth2ResourceServer { it.accessDeniedHandler(this::handleErrorResponse)
                .authenticationEntryPoint(this::handleErrorResponse)
                .jwt { it.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()) }}
            .build()
    }

    @Throws(IOException::class)
    private fun handleErrorResponse(request: HttpServletRequest, response: HttpServletResponse, exception: Exception) {
        val status = customDefaultErrorAttributes.getHttpStatusError(exception)
        val errorAttributes = customDefaultErrorAttributes.getErrorAttributes(ServletWebRequest(request), ErrorAttributeOptions.defaults())
        errorAttributes["message"] = exception.localizedMessage
        errorAttributes["status"] = status.value()
        response.status = status.value()
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        response.writer.append(objectMapper.writeValueAsString(errorAttributes))
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter? {
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("authorities")
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("")
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

}
