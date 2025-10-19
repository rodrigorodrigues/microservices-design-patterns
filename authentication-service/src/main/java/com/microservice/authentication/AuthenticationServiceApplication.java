package com.microservice.authentication;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.microservice.authentication.config.SpringSecurityConfiguration;
import com.microservice.web.common.util.constants.DefaultUsers;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@EnableAsync
@ImportRuntimeHints(AuthenticationRuntimeHints.class)
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({SpringSecurityConfiguration.RegistrationProperties.class, SpringSecurityConfiguration.WebAuthnProperties.class})
public class AuthenticationServiceApplication implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
		SpringApplication.run(AuthenticationServiceApplication.class, args);
	}

    @Bean
    @ConditionalOnMissingBean
    public SessionRepository defaultSessionRepository() {
        return new MapSessionRepository(new HashMap<>());
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @ConditionalOnProperty(prefix = "com.microservice.authentication.jwt", name = "key-value")
    @Primary
    @ConditionalOnMissingBean
    @Bean
    public JwtDecoder jwtDecoder(AuthenticationProperties properties) throws JOSEException {
        AuthenticationProperties.Jwt jwt = properties.getJwt();
        byte[] secret = jwt.getKeyValue().getBytes(StandardCharsets.UTF_8);

        MACSigner macSigner = new MACSigner(secret);
        return NimbusJwtDecoder.withSecretKey(macSigner.getSecretKey()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    JwtEncoder jwtEncoder(AuthenticationProperties properties) {
        AuthenticationProperties.Jwt jwt = properties.getJwt();
        String keyValue = jwt.getKeyValue();
        if (StringUtils.isNotBlank(keyValue)) {
            return parameters -> {
                byte[] secret = jwt.getKeyValue().getBytes(StandardCharsets.UTF_8);
                SecretKeySpec secretKeySpec = new SecretKeySpec(secret, "HMACSHA256");

                try {
                    MACSigner signer = new MACSigner(secretKeySpec);

                    JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
                    parameters.getClaims().getClaims().forEach((key, value) ->
                        claimsSetBuilder.claim(key, value instanceof Instant ? Date.from((Instant) value) : value)
                    );
                    JWTClaimsSet claimsSet = claimsSetBuilder.build();

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("alg", JWSAlgorithm.HS256.getName());
                    jsonObject.put("typ", "JWT");

                    JWSHeader header = JWSHeader.parse(jsonObject);
                    SignedJWT signedJWT = new SignedJWT(header, claimsSet);
                    signedJWT.sign(signer);

                    return Jwt.withTokenValue(signedJWT.serialize())
                        .header("alg", header.getAlgorithm().getName())
                        .header("typ", "JWT")
                        .subject(claimsSet.getSubject())
                        .issuer(claimsSet.getIssuer())
                        .claims(claims -> claims.putAll(claimsSet.getClaims()))
                        .issuedAt(claimsSet.getIssueTime().toInstant())
                        .expiresAt(claimsSet.getExpirationTime().toInstant())
                        .build();
                }
                catch (Exception e) {
                    throw new IllegalStateException("Error while signing the JWT", e);
                }
            };
        } else {
            KeyPair keyPair = applicationContext.getBean(KeyPair.class);

            JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .build();
            JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
            return new NimbusJwtEncoder(jwks);
        }
    }

    @ConditionalOnProperty(prefix = "com.microservice.authentication.jwt", name = "key-value")
    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> jwkSource(AuthenticationProperties properties) {
        AuthenticationProperties.Jwt jwt = properties.getJwt();
        byte[] secret = jwt.getKeyValue().getBytes(StandardCharsets.UTF_8);

        OctetSequenceKey octetKey = new OctetSequenceKey.Builder(secret)
            .keyID(UUID.randomUUID().toString())
            .algorithm(JWSAlgorithm.HS256)
            .build();

        JWKSet jwkSet = new JWKSet(octetKey);

        return (jwkSelector, context) -> jwkSelector.select(jwkSet);
    }

    @Bean
    @ConditionalOnMissingBean
    BuildProperties buildProperties() {
        return new BuildProperties(new Properties());
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(AuthenticationCommonRepository authenticationCommonRepository,
                             PasswordEncoder passwordEncoder,
                             MongoTemplate mongoTemplate) {
        return args -> {
            if (authenticationCommonRepository.findByEmail(DefaultUsers.SYSTEM_DEFAULT.getValue()).isEmpty()) {
                Authentication authentication = Authentication.builder()
                    .email(DefaultUsers.SYSTEM_DEFAULT.getValue())
                    .password(passwordEncoder.encode("noPassword"))
                    .fullName("System Administrator")
                    .enabled(false)
                    .id(UUID.randomUUID().toString())
                    .build();
                log.debug("Creating default authentication: {}", authentication);
                authentication = mongoTemplate.save(authentication, "users_login");
                log.debug("Created Default Authentication: {}", authentication);
            }
            if (authenticationCommonRepository.findByEmail("admin@gmail.com").isEmpty()) {
                Authentication authentication = Authentication.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("P@ssword2020!"))
                    .fullName("Admin")
                    .enabled(true)
                    .id(UUID.randomUUID().toString())
                    .authorities(Collections.singletonList(new Authority("ROLE_ADMIN")))
                    .build();
                log.debug("Creating admin authentication: {}", authentication);
                authentication = mongoTemplate.save(authentication, "users_login");
                log.debug("Created admin Authentication: {}", authentication);
            }
        };
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");
        serializer.setCookiePath("/");
        serializer.setUseBase64Encoding(false);
        return serializer;
    }

	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener() {
		return new ValidatingMongoEventListener(validator());
	}

	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}

    @ConditionalOnMissingBean
    @Bean
    LogoutSuccessHandler customLogoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler();
    }

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

@Slf4j
@Controller
@AllArgsConstructor
class HomeController {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @GetMapping("/")
    @ResponseBody
    public Authentication user(org.springframework.security.core.Authentication authentication) {
        log.debug("Logged user: {}", authentication);
        if (authentication instanceof OAuth2AuthenticationToken oauth2) {
            DefaultOidcUser oidcIdToken = (DefaultOidcUser) oauth2.getPrincipal();
            Optional<Authentication> findById = authenticationCommonRepository.findByEmail(oidcIdToken.getEmail());
            return findById.orElseGet(() -> authenticationCommonRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found user: " + authentication.getName())));
        } else {
            Optional<Authentication> findById = authenticationCommonRepository.findById(authentication.getName());
            return findById.orElseGet(() -> authenticationCommonRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found user: " + authentication.getName())));
        }
    }
}
