package com.microservice.authentication.autoconfigure;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.microservice.authentication")
public class AuthenticationProperties {
    private String issuer = "https://spendingbetter.com";
    private String aud = "https://spendingbetter.com";
    private String kid = "test";

    private Jwk jwk = new Jwk();
    private Jwt jwt = new Jwt();

    @Data
    public class Jwt {

        /**
         * The verification key of the JWT token. Can either be a symmetric secret or
         * PEM-encoded RSA public key. If the value is not available, you can set the URI
         * instead.
         */
        private String keyValue;

        /**
         * The URI of the JWT token. Can be set if the value is not available and the key
         * is public.
         */
        private String keyUri;

        /**
         * The location of the key store.
         */
        private String keyStore;

        /**
         * The location of the public key store.
         */
        private String publicKeyStore;

        /**
         * The key store's password
         */
        private String keyStorePassword;

        /**
         * The alias of the key from the key store
         */
        private String keyAlias;

        /**
         * The password of the key from the key store
         */
        private String keyPassword;
    }

    @Data
    public class Jwk {
        /**
         * The URI to get verification keys to verify the JWT token. This can be set when
         * the authorization server returns a set of verification keys.
         */
        private String keySetUri;
    }
}
