package com.pharmacy.auth;

public record LoginRequest(String username, String password) {
    public LoginRequest {
        if (username != null)
            username = username.trim();
        if (password != null)
            password = password.trim();
    }

}
