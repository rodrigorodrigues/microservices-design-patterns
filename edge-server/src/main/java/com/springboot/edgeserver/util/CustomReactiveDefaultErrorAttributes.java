package com.springboot.edgeserver.util;


import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom Error Attributes.
 */
@Slf4j
@Component
public class CustomReactiveDefaultErrorAttributes extends DefaultErrorAttributes {

    /**
     * Return custom error message according to exception.
     * @param request the current request
     * @param options injected by spring
     * @return error message
     */
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        Throwable error = getError(request);
        if (error != null) {
            HttpStatusCode status = getHttpStatusError(error);
            errorAttributes.put("status", status.value());
            errorAttributes.put("message", ExceptionUtils.getMessage(error));
            errorAttributes.put("error", status);
            log.error("Found error: {}", errorAttributes, error);
        } else {
            log.debug("Default Error Attributes: {}", errorAttributes);
        }
        return errorAttributes;
    }

    /**
     * Return {@link HttpStatus} according to type of Exception.
     * @param ex current exception
     * @return httpStatus
     */
    public HttpStatusCode getHttpStatusError(Throwable ex) {
        HttpStatusCode httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        if (ex instanceof HttpStatusCodeException e) {
            httpStatus = e.getStatusCode();
        } else if (ex instanceof ResponseStatusException e) {
            httpStatus = e.getStatusCode();
        } else if (ex instanceof AuthenticationException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ConstraintViolationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return httpStatus;
    }

}
