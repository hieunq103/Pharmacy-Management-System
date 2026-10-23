package com.pharmacy.employee;

import com.pharmacy.common.PageResult;
import java.util.List;
import java.util.Optional;

public interface EmployeeService {

    PageResult<Employee> getEmployeesPage(int page, int size, String keyword, String gender, String role, String status, String accountStatus);
    EmployeeRepository.EmployeeSummary getEmployeeSummary();
    Optional<Employee> findByIdWithDetails(Integer id);
    boolean createEmployeeWithAccount(Employee employee, String role);
    boolean markAsInactive(Integer employeeId);
    boolean lockAccount(Integer accountId);
    boolean unlockAccount(Integer accountId);
    List<Object[]> getLoginHistory(Integer accountId);
    boolean updateEmployeeByManager(EmployeeUpdateRequest request);
}