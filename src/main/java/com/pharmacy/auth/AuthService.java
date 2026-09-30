package com.pharmacy.auth;

import com.pharmacy.account.Account;
import java.util.Optional;

public interface AuthService {
    Account login(LoginRequest request);
    void logout();
    Optional<Account> getCurrentUser();
    boolean isAuthenticated();
}