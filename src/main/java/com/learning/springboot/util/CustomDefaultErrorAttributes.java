package com.learning.springboot.util;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
@AllArgsConstructor
public class CustomDefaultErrorAttributes extends DefaultErrorAttributes {
    private final HandleResponseError handleResponseError;

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, includeStackTrace);
        Throwable error = getError(request);
        HttpStatus status = handleResponseError.getHttpStatusError(error);
        errorAttributes.put("status", status.value());
        errorAttributes.put("message", ExceptionUtils.getMessage(error));
        errorAttributes.put("error", status);
        return errorAttributes;
    }
}
