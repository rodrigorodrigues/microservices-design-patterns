package com.microservice.web.common.util;

import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolationException;
import java.util.Map;

/**
 * Custom Error Attributes.
 */
@Slf4j
public class CustomDefaultErrorAttributes extends DefaultErrorAttributes {

    /**
     * Return custom error message according to exception.
     * @param request the current request
     * @param includeStackTrace set false
     * @return error message
     */
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, false);
        Throwable error = getError(request);
        HttpStatus status = getHttpStatusError(error);
        errorAttributes.put("status", status.value());
        errorAttributes.put("message", ExceptionUtils.getMessage(error));
        errorAttributes.put("error", status);
        log.debug("Default Error Attributes: {}", errorAttributes);
        return errorAttributes;
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
        } else if (ex instanceof ResponseStatusException) {
            httpStatus = ((ResponseStatusException) ex).getStatus();
        } else if (ex instanceof AuthenticationException || ex instanceof SignatureException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ConstraintViolationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }
        return httpStatus;
    }

}
