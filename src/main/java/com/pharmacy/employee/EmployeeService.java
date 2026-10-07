package com.pharmacy.employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {

    List<Employee> getAllEmployees();
    Optional<Employee> findByIdWithDetails(Integer id);
    boolean createEmployeeWithAccount(Employee employee, String role);
    boolean markAsInactive(Integer employeeId);
    boolean lockAccount(Integer accountId);
    boolean unlockAccount(Integer accountId);
    List<Object[]> getLoginHistory(Integer accountId);
    boolean updateEmployeeByManager(EmployeeUpdateRequest request);
}