package com.microservice.authentication.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.microservice.authentication")
public class AuthenticationProperties {
    private String issuer = "https://spendingbetter.com";
    private String aud = "https://spendingbetter.com";

    private Jwt jwt = new Jwt();

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

        private boolean enabledPublicKey;

        public String getKeyValue() {
            return this.keyValue;
        }

        public void setKeyValue(String keyValue) {
            this.keyValue = keyValue;
        }

        public void setKeyUri(String keyUri) {
            this.keyUri = keyUri;
        }

        public String getKeyUri() {
            return this.keyUri;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        void setEnabledPublicKey(boolean enabledPublicKey) {
            this.enabledPublicKey = enabledPublicKey;
        }

        boolean isEnabledPublicKey() {
            return enabledPublicKey;
        }

        void setPublicKeyStore(String publicKeyStore) {
            this.publicKeyStore = publicKeyStore;
        }

        String getPublicKeyStore() {
            return publicKeyStore;
        }
    }
}
