package com.learning.java8;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OptionalClass {
    public static void main(String[] args) {
        String opt = Optional.ofNullable(convertIntegerToString(1))
                .orElseThrow(() -> new IllegalArgumentException("Invalid number!"));

        log.debug("opt: {}", opt);

        Optional.ofNullable(convertIntegerToString(0))
                .orElseThrow(() -> new IllegalArgumentException("Invalid number!"));
    }

    static String convertIntegerToString(int number) {
        return (number > 0 ? String.valueOf(number) : null);
    }
}
