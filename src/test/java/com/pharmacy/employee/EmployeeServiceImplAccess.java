package com.pharmacy.employee;

import java.time.LocalDate;

public final class EmployeeServiceImplAccess {
    private EmployeeServiceImplAccess() {
    }

    public static void validate(String fullName, String phone, String nationalId, LocalDate dob) {
        EmployeeServiceImpl.validateEmployee(fullName, phone, nationalId, dob);
    }
}
