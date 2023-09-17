package com.microservice.kotlin.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.web.common.util.CustomDefaultErrorAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.context.request.ServletWebRequest
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SpringSecurityConfiguration(@Autowired val customDefaultErrorAttributes: CustomDefaultErrorAttributes,
                                  @Autowired val objectMapper: ObjectMapper,
                                  @Autowired val properties: AuthenticationProperties) : ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext

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
            .headers()
            .frameOptions().disable()
            .cacheControl().disable()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .authorizeHttpRequests()
            .requestMatchers(*WHITE_LIST).permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
            .accessDeniedHandler(this::handleErrorResponse)
            .authenticationEntryPoint(this::handleErrorResponse)
            .jwt {
                val jwtDecoder = jwtDecoder(properties)
                it.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter())
            }.and().build()
    }

    @Bean
    fun jwtDecoder(properties: AuthenticationProperties): JwtDecoder? {
        val jwt = properties.jwt
        return if (jwt != null && jwt.keyValue != null) {
            val secretKeySpec = SecretKeySpec(jwt.keyValue.toByteArray(StandardCharsets.UTF_8), "HS256")
            NimbusJwtDecoder.withSecretKey(secretKeySpec).build()
        } else {
            val publicKey = applicationContext.getBean(RSAPublicKey::class.java)
            NimbusJwtDecoder.withPublicKey(publicKey).build()
        }
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

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

}
