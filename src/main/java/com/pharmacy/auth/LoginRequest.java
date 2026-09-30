package com.pharmacy.auth;

public record LoginRequest(String identifier, String password) {
    public LoginRequest {
        if (identifier != null) identifier = identifier.trim();
        if (password != null) password = password.trim();
    }
}