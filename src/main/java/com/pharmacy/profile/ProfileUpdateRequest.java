package com.pharmacy.profile;

/**
 * Cập nhật hồ sơ cá nhân của chính người đang đăng nhập.
 * Cả Nhân viên và Quản lý chỉ được đổi Họ và tên, Số điện thoại, Địa chỉ của mình;
 * các thông tin khác do Quản lý chỉnh ở màn hình Quản lý nhân viên.
 */
public record ProfileUpdateRequest(
        Integer employeeId,
        String fullName,
        String phone,
        String address
) {
}
