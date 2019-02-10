package com.learning.springboot.util;

import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;

/**
 * Handle http status according to type of Exception.
 */
@Slf4j
@Component
public class HandleResponseError {
    /**
     * Set http status according to Exception and print exception message when writeToResponse is true.
     * @param exchange the current request
     * @param ex the current exception
     * @param writeToResponse print exception message to response when is true
     * @return Mono<Void>
     */
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex, boolean writeToResponse) {
        log.error("Error on calling api", ex);
        log.error("Error on calling api:request: {}", exchange.getRequest().getPath().value());
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus httpStatus = getHttpStatusError(ex);
        response.setStatusCode(httpStatus);
        if (writeToResponse) {
            byte[] bytes = ExceptionUtils.getMessage(ex).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Flux.just(buffer));
        } else {
            return Mono.empty();
        }
    }

    /**
     * Return {@link HttpStatus} according to type of Exception.
     * @param ex current exception
     * @return httpStatus
     */
    public HttpStatus getHttpStatusError(Throwable ex) {
        HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        if (ex instanceof HttpStatusCodeException) {
            httpStatus = ((HttpStatusCodeException) ex).getStatusCode();
        } else if (ex instanceof AuthenticationException || ex instanceof SignatureException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ConstraintViolationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return httpStatus;
    }
}
