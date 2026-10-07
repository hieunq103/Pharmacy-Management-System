package com.pharmacy.employee;

import java.time.LocalDate;

public record EmployeeUpdateRequest(
        Integer employeeId,
        String fullName,
        String nationalId,
        String phone,
        String address,
        LocalDate dob,
        String gender,
        String empStatus,
        String accountStatus,
        String roleName
) {}