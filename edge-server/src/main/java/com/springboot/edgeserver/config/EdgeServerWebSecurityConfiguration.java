package com.springboot.edgeserver.config;

import com.springboot.edgeserver.util.HandleResponseError;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring Security Configuration
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class EdgeServerWebSecurityConfiguration {
    private final HandleResponseError handleResponseError;
    private final OAuth2ClientProperties clientProperties;
    private final ReactiveSessionRepository sessionRepository;
    private final UserDetailsService userDetailsService;
    private final JavaMailSender javaMailSender;
    private static final String[] WHITELIST = {
            // -- swagger ui
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/swagger/**",
            "/swagger-ui/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/*.js",
            "/*.css",
            "/*.html",
            "/favicon.ico",
            // other public endpoints of your API may be appended to this array
            "/actuator/info",
            "/actuator/gateway/**",
            "/actuator/health/**",
            "/actuator/prometheus",
            "/error",
            "/api/**",
            "/oauth2/**",
            "/.well-known/**",
            "/swagger/**",
            "/login",
            "/admin/**",
            "/login/**",
            "/webauthn/**",
            "/ott/**"
    };

    /*@ConfigurationProperties(prefix = "baeldung.security.client.registration")
    public record RegistrationProperties (
        URI registrationEndpoint,
        String registrationUsername,
        String registrationPassword,
        List<String> registrationScopes,
        List<String> grantTypes,
        List<String> redirectUris,
        URI tokenEndpoint
    ) {}*/

    public EdgeServerWebSecurityConfiguration(HandleResponseError handleResponseError,
            OAuth2ClientProperties clientProperties,
            ReactiveSessionRepository sessionRepository,
            UserDetailsService userDetailsService,
            JavaMailSender javaMailSender) {
        this.handleResponseError = handleResponseError;
        this.clientProperties = clientProperties;
        this.sessionRepository = sessionRepository;
        this.userDetailsService = userDetailsService;
        this.javaMailSender = javaMailSender;
    }

    /*@Bean
    ReactiveClientRegistrationRepository dynamicClientRegistrationRepository( WebClient webClient) {

        log.info("Creating a dynamic client registration repository");

        var registrationDetails = new DynamicClientRegistrationRepository.RegistrationDetails(
                registrationProperties.registrationEndpoint(),
                registrationProperties.registrationUsername(),
                registrationProperties.registrationPassword(),
                registrationProperties.registrationScopes(),
                registrationProperties.grantTypes(),
                registrationProperties.redirectUris(),
                registrationProperties.tokenEndpoint());

        // Use standard client registrations as
        Map<String, ClientRegistration> staticClients = (new OAuth2ClientPropertiesMapper(clientProperties)).asClientRegistrations();
        var repo = new DynamicClientRegistrationRepository(registrationDetails, staticClients, webClient);
        repo.doRegistrations();
        return repo;
    }*/

    @Bean
    WebClient registrationRestTemplate(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }

    /*@Bean
    public OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository repo) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }*/

    /*@Bean
    public ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(ReactiveClientRegistrationRepository dynamicClientRegistrationRepository) {
        ServerWebExchangeMatcher authorizationRequestMatcher =
                new PathPatternParserServerWebExchangeMatcher(
                        "/login/oauth2/authorization/{registrationId}");

        return new DefaultServerOAuth2AuthorizationRequestResolver(dynamicClientRegistrationRepository, authorizationRequestMatcher);
    }*/

    @Bean
    public SecurityWebFilterChain configure(ServerHttpSecurity http, @Value("${TOKEN_HOST:http://localhost:8081}") String tokenHost
            //ServerOAuth2AuthorizationRequestResolver resolver
    ) {
        return http
                .csrf(c -> c.disable().headers(h -> h.frameOptions(f -> f.disable().cache(ServerHttpSecurity.HeaderSpec.CacheSpec::disable))))
                .authorizeExchange(a -> a.pathMatchers(WHITELIST).permitAll()
                        .pathMatchers("/actuator/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                /*.oneTimeTokenLogin(c -> c.tokenGenerationSuccessHandler((exchange, oneTimeToken) -> {
                    com.microservice.authentication.common.model.Authentication authentication = (com.microservice.authentication.common.model.Authentication) userDetailsService.loadUserByUsername(oneTimeToken.getUsername());

                    var msg = String.format("go to %s/login/ott?token=%s<br>you've got console mail!", tokenHost, oneTimeToken.getTokenValue());
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom("noreply@spendingbetter.com");
                    message.setTo(authentication.getEmail());
                    message.setSubject("One Time Login");
                    message.setText(msg);
                    javaMailSender.send(message);
                    System.out.println(msg);
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.OK);
                    byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                    DataBuffer buffer = response.bufferFactory().wrap(bytes);
                    return response.writeAndFlushWith(Flux.just(Mono.just(buffer)));
                }))*/
                //.oauth2Login(a -> a.authorizationRequestResolver(resolver))
                //.oauth2Client(Customizer.withDefaults())
                .exceptionHandling(exception -> exception.accessDeniedHandler((exchange, denied) -> handleResponseError.handle(exchange, denied, true)))
/*
                .with(webauthn() ,c -> c
                        .allowedOrigins("*")
                        .rpId("localhost")
                        .rpName("Bootiful Passkeys")
                )
*/
                .formLogin(c -> c.authenticationSuccessHandler((webFilterExchange, authentication) -> webFilterExchange.getChain()
                                .filter(webFilterExchange.getExchange())
                                .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(authentication)))
                        .authenticationFailureHandler((webFilterExchange, exception) -> handleResponseError.handle(webFilterExchange.getExchange(), exception, true)))

                .logout(l -> l.logoutSuccessHandler(new RedirectServerLogoutSuccessHandler() {
                    @Override
                    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
                        log.info("Logout success! authType: {}", authentication.getClass().getName());
                        return sessionRepository.deleteById(exchange.getExchange().getRequest().getId())
                                .then(super.onLogoutSuccess(exchange, authentication));
                    }
                }))
                .build();
    }


    /*private Mono<Void> processAuthentication(WebFilterExchange webFilterExchange, Authentication authentication) {
        return webFilterExchange.getExchange().getSession()
                        .map(s -> {
                            SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
                            s.getAttributes().put(DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME, securityContext);
                            OAuth2AccessToken accessToken;
                            if (authentication instanceof OAuth2Authentication oAuth2Authentication) {
                                accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
                            } else {
                                OAuth2Request oAuth2Request = new OAuth2Request(null, authentication.getName(), authentication.getAuthorities(),
                                        true, Collections.singleton("read"), null, null, null, null);
                                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
                                accessToken = defaultTokenServices.createAccessToken(oAuth2Authentication);
                            }
                            log.info("login:set accessToken: {}={}", accessToken.getTokenType(), accessToken.getExpiration());
                            return securityContext;
                        })
                .subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .doOnSuccess(c -> log.info("login:Authenticated user in the session: {}", c.getAuthentication().getName()))
                .then();
    }*/
}