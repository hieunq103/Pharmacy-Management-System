package com.pharmacy.employee;

// Thông tin đăng nhập của nhân viên vừa tạo; mật khẩu tạm chỉ có ở đây, CSDL chỉ lưu bản băm
public record NewEmployeeAccount(String username, String temporaryPassword) {
}
