package com.learning.springboot.constants;

public enum DefaultUsers {
    SYSTEM_DEFAULT("default@admin.com");

    private final String value;

    DefaultUsers(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
