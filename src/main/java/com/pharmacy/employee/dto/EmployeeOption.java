package com.pharmacy.employee.dto;

public record EmployeeOption(Integer id, String code, String fullName) {
    public static final EmployeeOption ALL = new EmployeeOption(null, "", "Tất cả nhân viên");

    public String label() {
        return id == null ? fullName : code + " - " + fullName;
    }
}
