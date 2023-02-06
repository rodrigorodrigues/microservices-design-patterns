package com.springboot.edgeserver;

import java.net.URI;
import java.security.Principal;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import com.springboot.edgeserver.filters.AdminResourcesFilter;
import com.springboot.edgeserver.filters.AuthenticationPostFilter;
import com.springboot.edgeserver.filters.LogoutPostFilter;
import com.springboot.edgeserver.util.ReactivePreAuthenticatedAuthenticationManagerCustom;
import com.springboot.edgeserver.util.ReactiveSharedAuthenticationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Slf4j
@SpringBootApplication
@EnableWebFlux
@EnableDiscoveryClient
public class EdgeServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdgeServerApplication.class, args);
	}

	@Primary
	@Bean
	ReactiveUserDetailsService reactiveUserDetailsService(AuthenticationCommonRepository authenticationCommonRepository) {
		return new ReactiveSharedAuthenticationServiceImpl(authenticationCommonRepository);
	}

	@Primary
	@Bean
	ReactiveAuthenticationManager authenticationManager(ReactiveUserDetailsService reactiveUserDetailsService) {
		return new ReactivePreAuthenticatedAuthenticationManagerCustom(reactiveUserDetailsService);
	}

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("SESSIONID");
		serializer.setCookiePath("/");
		return serializer;
	}

    /*@Bean
    public RouteLocator cacheRequestBodyApiRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("cache_request_body_route", r -> r.method(HttpMethod.POST)
                .filters(f -> f.prefixPath("/api/**").cacheRequestBody(String.class)).uri("origin"))
                .build();
    }*/

	@Bean
	public RouteLocator adminApiRoutes(RouteLocatorBuilder builder,
        @Value("${grafanaUrl:http://localhost:3000/admin/grafana}") String grafanaUrl,
        @Value("${prometheusUrl:http://localhost:9090/amin/prometheus/graph}") String prometheusUrl,
        @Value("${jaegerUrl:http://localhost:16686/admin/jaeger}") String jaegerUrl,
			AuthenticationPostFilter authenticationPostFilter,
			AdminResourcesFilter adminResourcesFilter,
			LogoutPostFilter logoutPostFilter) {
		return builder.routes()
				.route("adminResourcesFilterGrafana", p -> p
						.path("/admin/grafana/**")
						.filters(f -> f.filter(adminResourcesFilter)
								.removeResponseHeader("X-Frame-Options")
								.addResponseHeader("X-Frame-Options", "sameorigin"))
						.uri(grafanaUrl))
                .route("adminResourcesFilterPrometheus", p -> p
                    .path("/admin/prometheus/**")
                    .filters(f -> f.filter(adminResourcesFilter)
							.removeResponseHeader("X-Frame-Options")
							.addResponseHeader("X-Frame-Options", "sameorigin"))
						.uri(prometheusUrl))
                .route("adminResourcesFilterJaeger", p -> p
                    .path("/admin/jaeger/**")
                    .filters(f -> f.filter(adminResourcesFilter)
							.removeResponseHeader("X-Frame-Options")
							.addResponseHeader("X-Frame-Options", "sameorigin"))
						.uri(jaegerUrl))
                .route("adminResourcesFilterAuth", p -> p
                    .path("/login/oauth2/**", "/oauth2/**", "/api/authenticate", "/oauth/**")
                    .filters(f -> f.filter(authenticationPostFilter))
                    .uri("lb://authentication-service"))
				.route("logoutPostFilter", p -> p
						.path("/api/logout")
						.filters(f -> f.filter(logoutPostFilter))
						.uri("lb://authentication-service"))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> router() {
		return route(GET("/"), req -> ServerResponse.temporaryRedirect(URI.create("/actuator")).build())
				.andRoute(POST("/api/authenticationBearerToken"), request -> request.principal()
						.map(Principal::getName)
						.flatMap(u -> ServerResponse.ok().bodyValue(u))
						.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Could not find a valid token"))));
	}

	/*@Bean
	public RouterFunction<ServerResponse> router() {
			log.info("Calling /api/authenticationBearerToken");
			String authorizationHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
			if (StringUtils.isBlank(authorizationHeader)) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not found authorization header");
			}
			OAuth2Authentication authentication = redisTokenStore.readAuthentication(authorizationHeader.replaceFirst("(?i)Bearer ", ""));
			log.info("Generate authentication: {}", authentication);
			return ServerResponse.ok().build();
			return ReactiveSecurityContextHolder.getContext()
					.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ReactiveSecurityContext is empty")))
					.flatMap(s -> {
						log.info("Setting authentication to securityContext");
						s.setAuthentication(authentication);
						return Mono.just(authentication);
					})
					.doOnError(Throwable::printStackTrace)
					.doOnSuccess(s -> log.info("completed authentication: {}", s))
					.flatMap(s -> ServerResponse.ok().build());
		//});
	}*/

}

