package com.pharmacy.profile;

public record ProfileUpdateRequest(
        Integer employeeId,
        String phone,
        String address
) {
    public ProfileUpdateRequest {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }

        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        phone = phone.trim();

        if (!phone.matches("^0[35789]\\d{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ không được để trống.");
        }
        address = address.trim();
        if (address.length() > 255) {
            throw new IllegalArgumentException("Địa chỉ không được vượt quá 255 ký tự.");
        }
    }
}