package com.pharmacy.employee.dto;

public record EmployeeSummary(long totalEmployees, long activeEmployees) {
    public long total() {
        return totalEmployees;
    }

    public long active() {
        return activeEmployees;
    }
}

