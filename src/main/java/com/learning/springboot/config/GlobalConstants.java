package com.learning.springboot.config;

import lombok.AllArgsConstructor;

/**
 * Global constants.
 */
@AllArgsConstructor
public enum GlobalConstants {
    JWT("jwtSession");

    private final String name;

    @Override
    public String toString() {
        return this.name;
    }
}
