package com.learning.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Component
public class GlobalExceptionHandler extends DefaultErrorWebExceptionHandler {
    private final HandleResponseError handleResponseError;

    /**
     * Create a new {@code DefaultErrorWebExceptionHandler} instance.
     *  @param errorAttributes    the error attributes
     * @param resourceProperties the resources configuration properties
     * @param serverProperties    the server configuration properties
     * @param applicationContext the current application context
     * @param handleResponseError
     */
    public GlobalExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
                                  ServerProperties serverProperties, ApplicationContext applicationContext,
                                  HandleResponseError handleResponseError, ObjectProvider<ViewResolver> viewResolversProvider,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, resourceProperties, serverProperties.getError(), applicationContext);
        super.setViewResolvers(viewResolversProvider.orderedStream()
                .collect(Collectors.toList()));
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        this.handleResponseError = handleResponseError;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.debug("Error Response Before: {}", exchange.getResponse().getStatusCode());
        handleResponseError.handle(exchange, ex, false);
        log.debug("Error Response After: {}", exchange.getResponse().getStatusCode());
        return super.handle(exchange, ex);
    }
}
