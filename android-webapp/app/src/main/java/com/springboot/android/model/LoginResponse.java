package com.springboot.android.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("tokenValue")
    private String tokenValue;

    @SerializedName("tokenType")
    private TokenType tokenType;

    @SerializedName("issuedAt")
    private String issuedAt;

    @SerializedName("expiresAt")
    private String expiresAt;

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public static class TokenType {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
