package com.microservice.authentication.web.util;

import io.jsonwebtoken.security.SignatureException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class CustomDefaultErrorAttributes extends DefaultErrorAttributes {
    public Map<String, Object> getErrorAttributes(HttpServletRequest request, Throwable throwable,  boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(new ServletWebRequest(request), includeStackTrace);
        HttpStatus status = getHttpStatusError(throwable);
        errorAttributes.put("status", status.value());
        errorAttributes.put("message", ExceptionUtils.getMessage(throwable));
        errorAttributes.put("error", status);
        log.debug("Default Error Attributes: {}", errorAttributes);
        return errorAttributes;
    }

    /**
     * Return {@link HttpStatus} according to type of Exception.
     * @param ex current exception
     * @return httpStatus
     */
    private HttpStatus getHttpStatusError(Throwable ex) {
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
