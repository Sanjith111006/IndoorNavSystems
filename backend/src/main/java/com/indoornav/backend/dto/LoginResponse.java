package com.indoornav.backend.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {

    /** Hides the token so it cannot reach a log through this record. */
    @Override
    public String toString() {
        return "LoginResponse[tokenType=" + tokenType + "]";
    }
}
