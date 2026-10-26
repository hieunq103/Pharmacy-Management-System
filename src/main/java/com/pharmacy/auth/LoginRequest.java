package com.pharmacy.auth;

public record LoginRequest(String username, String password) {
    public LoginRequest {
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();
    }

    /**
     * Phương thức hỗ trợ tương thích nếu còn lời gọi cũ.
     */
    public String identifier() {
        return username;
    }
}