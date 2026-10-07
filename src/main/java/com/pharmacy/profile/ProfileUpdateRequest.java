package com.pharmacy.profile;

import java.time.LocalDate;

public record ProfileUpdateRequest(
        Integer employeeId,
        String phone,
        String address,
        String fullName,
        String nationalId,
        LocalDate dob,
        String gender,
        boolean isManagerUpdate
)  {

    public ProfileUpdateRequest(Integer employeeId, String phone, String address) {
        this(employeeId, phone, address, null, null, null, null, false);
    }

    public ProfileUpdateRequest(Integer employeeId, String phone, String address,
                                String fullName, String nationalId, LocalDate dob, String gender) {
        this(employeeId, phone, address, fullName, nationalId, dob, gender, true);
    }
}