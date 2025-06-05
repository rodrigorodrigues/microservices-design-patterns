package com.microservice.web.autoconfigure;

import java.net.URL;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector;
import com.nimbusds.jwt.proc.JWTProcessor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer {
    @Autowired
    private Environment environment;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui.html**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/swagger-resources")
                .addResourceLocations("classpath:/META-INF/swagger-resources");

        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/swagger-resources/**");

        registry.addResourceHandler("/configuration/ui")
                .addResourceLocations("classpath:/META-INF/configuration/ui");

        registry.addResourceHandler("/configuration/security")
                .addResourceLocations("classpath:/META-INF/configuration/security");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Profile("cors")
    @Primary
    @Bean
    CorsFilter corsWebFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Profile("cors")
    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        if (environment.acceptsProfiles(Profiles.of("prod")) && !environment.acceptsProfiles(Profiles.of("consul"))) {
            corsConfig.addAllowedOrigin("https://spendingbetter.com");
        } else {
            corsConfig.addAllowedOriginPattern("*");
        }
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }


    @Bean
    JWTProcessor jwtProcessor(JWTClaimsSetAwareJWSKeySelector keySelector) {
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor =
                new DefaultJWTProcessor();
        jwtProcessor.setJWTClaimsSetAwareJWSKeySelector(keySelector);
        return jwtProcessor;
    }

    @ConditionalOnMissingBean
    @Bean
    JwtDecoder jwtDecoder(JWTProcessor jwtProcessor, OAuth2TokenValidator<Jwt> jwtValidator) {
        NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>
                (JwtValidators.createDefault(), jwtValidator);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    TenantRepository tenantRepository(WebConfigurationProperties properties) {
        return new TenantRepository(properties);
    }

    @Bean
    OAuth2TokenValidator<Jwt> oAuth2TokenValidator(TenantRepository tenantRepository) {
        return new TenantJwtIssuerValidator(tenantRepository);
    }

    @Bean
    JWTClaimsSetAwareJWSKeySelector<SecurityContext> jwtClaimsSetAwareJWSKeySelector(TenantRepository tenantRepository) {
        return new TenantJWSKeySelector(tenantRepository);
    }


    private class TenantJwtIssuerValidator implements OAuth2TokenValidator<Jwt> {
        private final TenantRepository tenants;

        private final OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The iss claim is not valid",
                "https://tools.ietf.org/html/rfc6750#section-3.1");

        public TenantJwtIssuerValidator(TenantRepository tenants) {
            this.tenants = tenants;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if(this.tenants.findById(token.getIssuer()) != null) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(this.error);
        }
    }

    private class TenantJWSKeySelector
            implements JWTClaimsSetAwareJWSKeySelector<SecurityContext> {

        private final TenantRepository tenants;
        private final Map<String, JWSKeySelector<SecurityContext>> selectors = new ConcurrentHashMap<>();

        public TenantJWSKeySelector(TenantRepository tenants) {
            this.tenants = tenants;
        }

        @Override
        public List<? extends Key> selectKeys(JWSHeader jwsHeader, JWTClaimsSet jwtClaimsSet, SecurityContext securityContext)
                throws KeySourceException {
            return this.selectors.computeIfAbsent(toTenant(jwtClaimsSet), this::fromTenant)
                    .selectJWSKeys(jwsHeader, securityContext);
        }

        private String toTenant(JWTClaimsSet claimSet) {
            return (String) claimSet.getClaim("iss");
        }

        private JWSKeySelector<SecurityContext> fromTenant(String tenant) {
            return Optional.ofNullable(this.tenants.findById(tenant))
                    .map(this::fromUri)
                    .orElseThrow(() -> new IllegalArgumentException("unknown tenant"));
        }

        private JWSKeySelector<SecurityContext> fromUri(String uri) {
            try {
                return JWSAlgorithmFamilyJWSKeySelector.fromJWKSetURL(new URL(uri));
            } catch (Exception ex) {
                log.warn("jwks error from uri = {}", uri, ex);
                throw new IllegalArgumentException(ex);
            }
        }
    }

    private class TenantRepository {
        private final Map<String, String> tenants;

        private TenantRepository(WebConfigurationProperties properties) {
            this.tenants = properties.getTenants();
        }

        public String findById(URL url) {
            log.info("TenantRepository:findById:url: {}", url);
            return Optional.ofNullable(tenants.get(url.toString()))
                    .orElseThrow(() -> new IllegalArgumentException("unknown tenant: " + url));
        }

        public String findById(String id) {
            log.info("TenantRepository:findById:id: {}", id);
            return Optional.ofNullable(tenants.get(id))
                    .orElseThrow(() -> new IllegalArgumentException("unknown tenant: " + id));
        }
    }
}
