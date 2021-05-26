package com.microservice.user.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.web.common.util.CustomDefaultErrorAttributes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Spring Security Configuration
 */
@Slf4j
@AllArgsConstructor
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    private final ObjectMapper objectMapper;

    private final AuthenticationProperties properties;

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
        "/actuator/info",
        "/actuator/health",
        "/actuator/prometheus",
        "/error"
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
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
                .antMatchers(WHITELIST).permitAll()
                .antMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .oauth2ResourceServer()
                .accessDeniedHandler(this::handleErrorResponse)
                .authenticationEntryPoint(this::handleErrorResponse)
                .jwt(jwtConfigurer -> {
                    JwtDecoder jwtDecoder = getApplicationContext().getBean(JwtDecoder.class);
                    jwtConfigurer.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter());
                });
    }

    @Bean
    public JwtDecoder jwtDecoder(AuthenticationProperties properties) {
        AuthenticationProperties.Jwt jwt = properties.getJwt();
        if (jwt != null && jwt.getKeyValue() != null) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwt.getKeyValue().getBytes(StandardCharsets.UTF_8), "HS256");
            return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
        } else {
            RSAPublicKey publicKey = getApplicationContext().getBean(RSAPublicKey.class);
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        }
    }

    private void handleErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        HttpStatus status = customDefaultErrorAttributes.getHttpStatusError(exception);
        Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
        errorAttributes.put("message", exception.getLocalizedMessage());
        errorAttributes.put("status", status.value());
        response.setStatus(status.value());
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
