package com.microservice.kotlin.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.microservice.authentication.autoconfigure.AuthenticationProperties
import com.microservice.authentication.common.service.Base64DecodeUtil
import com.microservice.web.common.util.CustomDefaultErrorAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Profiles
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory
import org.springframework.web.context.request.ServletWebRequest
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.interfaces.RSAPublicKey
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.jvm.Throws

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SpringSecurityConfiguration(@Autowired val customDefaultErrorAttributes: CustomDefaultErrorAttributes,
                                  @Autowired val objectMapper: ObjectMapper,
                                  @Autowired val properties: AuthenticationProperties) : WebSecurityConfigurerAdapter() {
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
            .oauth2ResourceServer()
            .accessDeniedHandler(this::handleErrorResponse)
            .authenticationEntryPoint(this::handleErrorResponse)
            .jwt {
                val environment = applicationContext.environment
                val jwtDecoder = if (environment.acceptsProfiles(Profiles.of("prod"))) jwtDecoderProd(keyPair(properties)) else jwtDecoder(properties)
                it.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter())
            }
    }

    fun keyPair(properties: AuthenticationProperties): RSAPublicKey? {
        val jwt = properties.jwt
        val password = Base64DecodeUtil.decodePassword(jwt.keyStorePassword)
        val keyStoreKeyFactory = KeyStoreKeyFactory(FileSystemResource(jwt.keyStore.replaceFirst("file:".toRegex(), "")), password)
        return keyStoreKeyFactory.getKeyPair(jwt.keyAlias).public as RSAPublicKey
    }

    fun jwtDecoderProd(publicKey: RSAPublicKey?): JwtDecoder? {
        return NimbusJwtDecoder.withPublicKey(publicKey).build()
    }

    fun jwtDecoder(properties: AuthenticationProperties): JwtDecoder? {
        val secretKeySpec = SecretKeySpec(properties.jwt.keyValue.toByteArray(StandardCharsets.UTF_8), "HS256")
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).build()
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
