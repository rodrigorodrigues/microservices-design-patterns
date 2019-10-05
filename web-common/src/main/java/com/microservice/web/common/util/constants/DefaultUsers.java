package com.microservice.web.common.util.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DefaultUsers {
    SYSTEM_DEFAULT("default@admin.com");

    @Getter
    private final String value;
}
