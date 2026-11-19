package com.pharmacy.employee.dto;

/** Một dòng trong ô chọn nhân viên (mã + họ tên). {@code id == null} nghĩa là "Tất cả nhân viên". */
public record EmployeeOption(Integer id, String code, String fullName) {
    public static final EmployeeOption ALL = new EmployeeOption(null, "", "Tất cả nhân viên");

    public String label() {
        return id == null ? fullName : code + " - " + fullName;
    }
}
