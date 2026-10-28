package com.pharmacy.profile;

public record ProfileUpdateRequest(
        Integer employeeId,
        String fullName,
        String phone,
        String address
) {
}
