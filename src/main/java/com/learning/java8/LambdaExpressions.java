package com.learning.java8;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

@Slf4j
public class LambdaExpressions {

    public static void main(String[] args) {
        Lambda innerClass = new Lambda() {
            @Override
            public String getName() {
                return "Inner Class";
            }
        };
        innerClass.printName();

        Lambda lambda = () -> "Lambda Expressions";
        lambda.printName();

        Predicate predicate = new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.toLowerCase().contains("lambda");
            }
        };

        log.debug("Should be false: {}", predicate.test(innerClass.getName()));

        predicate = (Predicate<String>) s -> s.toLowerCase().contains("lambda");
        log.debug("Should be true: {}", predicate.test(lambda.getName()));
    }


    @FunctionalInterface
    interface Lambda {
        Logger log = LoggerFactory.getLogger(LambdaExpressions.class);

        String getName();

        default void printName() {
            log.debug("Print Name: {}", getName());
        }
    }
}
