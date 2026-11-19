package com.pharmacy.profile.dto;

public record ProfileUpdateRequest(Integer employeeId, String fullName, String phone, String address) {}
