package com.learning.springboot.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

/**
 * Custom Error Attributes.
 */
@Slf4j
@Component
@AllArgsConstructor
public class CustomDefaultErrorAttributes extends DefaultErrorAttributes {
    private final HandleResponseError handleResponseError;

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
        HttpStatus status = handleResponseError.getHttpStatusError(error);
        errorAttributes.put("status", status.value());
        errorAttributes.put("message", ExceptionUtils.getMessage(error));
        errorAttributes.put("error", status);
        log.debug("Default Error Attributes: {}", errorAttributes);
        return errorAttributes;
    }
}
