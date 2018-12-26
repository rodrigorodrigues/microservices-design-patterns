package com.learning.springboot.config;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum GlobalConstants {
    JWT("jwtSession"),
    AUTHORIZATION_HEADER("Authorization");

    private final String name;

    @Override
    public String toString() {
        return this.name;
    }
}
