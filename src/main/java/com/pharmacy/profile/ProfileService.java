package com.pharmacy.profile;

public interface ProfileService {
    void updateProfile(ProfileUpdateRequest request);
    void changePassword(String username, String currentPassword, String newPassword);
}