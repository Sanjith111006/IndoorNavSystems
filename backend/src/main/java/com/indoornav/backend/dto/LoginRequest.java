package com.indoornav.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 200) String password) {

    public LoginRequest {
        username = username == null ? null : username.strip();
    }

    /** Hides the password so it cannot reach a log through this record. */
    @Override
    public String toString() {
        return "LoginRequest[]";
    }
}
