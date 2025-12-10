package com.microservice.authentication.common.model;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import org.springframework.core.convert.converter.Converter;

@Slf4j
public enum UserType implements Converter<String, UserType> {
    GOOGLE,
    FACEBOOK,
    GITHUB,
    UNKNOWN;

    @Override
    public @NonNull UserType convert(String source) {
        try {
            return UserType.valueOf(source);
        } catch (Exception e) {
            log.error("Could not convert source: {} set as UNKNOWN", source, e);
            return UNKNOWN;
        }
    }
}
