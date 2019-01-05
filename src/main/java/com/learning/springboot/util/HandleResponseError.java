package com.learning.springboot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class HandleResponseError {
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex, boolean writeToResponse) {
        log.error("Error on calling api", ex);
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

    public HttpStatus getHttpStatusError(Throwable ex) {
        HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        if (ex instanceof AuthenticationCredentialsNotFoundException) {
            httpStatus = HttpStatus.FORBIDDEN;
        } else if (ex instanceof HttpStatusCodeException) {
            httpStatus = ((HttpStatusCodeException) ex).getStatusCode();
        } else if (ex instanceof AuthenticationException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ConstraintViolationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return httpStatus;
    }
}
