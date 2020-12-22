package com.microservice.authentication.common.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

@Slf4j
public class Base64DecodeUtil {
    public static char[] decodePassword(String password) {
        try {
            return new String(Base64.getDecoder().decode(password)).trim().toCharArray();
        } catch (Exception e) {
            log.warn("Could not decode to base64, use raw password", e);
            return password.toCharArray();
        }
    }
}
