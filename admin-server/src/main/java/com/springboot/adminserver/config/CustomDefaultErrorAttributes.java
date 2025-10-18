package com.springboot.adminserver.config;

import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class CustomDefaultErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        Throwable throwable = getError(request);
        if (throwable != null) {
            HttpStatusCode status = getHttpStatusError(throwable);
            errorAttributes.put("status", status.value());
            errorAttributes.put("message", ExceptionUtils.getMessage(throwable));
            errorAttributes.put("error", status);
        }
        log.debug("Default Error Attributes: {}", errorAttributes);
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
        } else if (ex instanceof AccessDeniedException) {
            httpStatus = HttpStatus.FORBIDDEN;
        }
        return httpStatus;
    }
}
