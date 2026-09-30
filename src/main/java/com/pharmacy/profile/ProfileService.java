package com.pharmacy.profile;

public interface ProfileService {
    void updateProfile(ProfileUpdateRequest request);
    void changePassword(String identifier, String currentPassword, String newPassword);
}