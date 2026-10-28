package com.pharmacy.employee;

public record EmployeeConflict(String code, String fullName, Employee.Status status) {

    // Nhân viên đã nghỉ vẫn giữ SĐT/CCCD trong hồ sơ cũ: hướng dẫn khôi phục hoặc sửa hồ sơ cũ
    public String message(String fieldLabel) {
        String who = code + " - " + fullName;
        if (status == Employee.Status.INACTIVE) {
            return fieldLabel + " này trùng với nhân viên đã nghỉ việc " + who + ". "
                    + "Nếu là người cũ quay lại làm, hãy mở hồ sơ " + code + " và đổi trạng thái về \"Đang làm việc\"; "
                    + "nếu không, hãy cập nhật " + fieldLabel.toLowerCase() + " trong hồ sơ " + code + " trước.";
        }
        return fieldLabel + " này đã thuộc về nhân viên " + who + ".";
    }
}
