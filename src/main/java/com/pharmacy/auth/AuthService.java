package com.pharmacy.auth;

import com.pharmacy.account.Account;

public interface AuthService {
    Account login(LoginRequest request);
    void logout();
}