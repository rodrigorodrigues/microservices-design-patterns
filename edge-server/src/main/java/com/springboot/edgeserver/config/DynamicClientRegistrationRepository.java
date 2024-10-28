package com.springboot.edgeserver.config;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Slf4j
public class DynamicClientRegistrationRepository implements ReactiveClientRegistrationRepository {

    private final RegistrationDetails registrationDetails;
    private final Map<String, ClientRegistration> staticClients;
    private final WebClient webClient;
    private final Map<String, Mono<ClientRegistration>> registrations = new HashMap<>();

    @Override
    public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
        log.info("findByRegistrationId: {}", registrationId);
        return registrations.computeIfAbsent(registrationId, this::doRegistration);
    }

    private Mono<ClientRegistration> doRegistration(String registrationId) {

        log.info("doRegistration: registrationId={}", registrationId);

        var staticRegistration = staticClients.get(registrationId);
        Assert.notNull(staticRegistration,"Invalid registrationId: " + registrationId);

        var body = Map.of(
                "client_name", staticRegistration.getClientName(),
                "grant_types", List.of(staticRegistration.getAuthorizationGrantType()),
                "scope", String.join(" ", staticRegistration.getScopes()),
                "redirect_uris", List.of(resolveCallbackUri(staticRegistration)));

        return createRegistrationToken()
                .flatMap(token -> webClient.post().uri(registrationDetails.registrationEndpoint()).body(BodyInserters.fromValue(body))
                        .headers(headers -> {
                            headers.setBearerAuth(token);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .retrieve()
                        .bodyToMono(ObjectNode.class)
                        .map(p -> createClientRegistration(staticRegistration, p)));
    }

    private String resolveCallbackUri(ClientRegistration registration) {

        var path = UriComponentsBuilder.fromUriString(registration.getRedirectUri())
                .build(Map.of(
                        "baseUrl", "",
                        "action", "login",
                        "registrationId", registration.getRegistrationId()))
                .toString();

        return "http://localhost:9998" + path;
    }

    private ClientRegistration createClientRegistration(ClientRegistration staticRegistration, ObjectNode body) {

        var clientId = body.get("client_id").asText();
        var clientSecret = body.get("client_secret").asText();

        log.info("creating ClientRegistration: registrationId={}, client_id={}", staticRegistration.getRegistrationId(),clientId);

        return ClientRegistration.withClientRegistration(staticRegistration)
                .clientId(body.get("client_id").asText())
                .clientSecret(body.get("client_secret").asText())
                .build();

    }

    private Mono<String> createRegistrationToken() {

        var body = new LinkedMultiValueMap<String,String>();
        body.put( "grant_type", List.of("client_credentials"));
        body.put(  "scope", registrationDetails.registrationScopes());

        return webClient.post().uri(registrationDetails.tokenEndpoint())
                .body(BodyInserters.fromValue(body))
                .headers(headers -> {
                    headers.setBasicAuth(registrationDetails.registrationUsername(), registrationDetails.registrationPassword());
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .retrieve()
                .bodyToMono(ObjectNode.class)
                .map(p -> {
                    log.debug("Access_token object: {}", p);
                    return p.get("access_token").asText();
                });
    }

    public void doRegistrations() {
        staticClients.forEach((key, value) -> findByRegistrationId(key).subscribe(c -> log.info("Created new client: {}", c)));
    }

    public record RegistrationDetails(
            URI registrationEndpoint,
            String registrationUsername,
            String registrationPassword,
            List<String> registrationScopes,
            List<String> grantTypes,
            List<String> redirectUris,
            URI tokenEndpoint
    ) {
    }
}