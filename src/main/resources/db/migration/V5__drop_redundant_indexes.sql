-- Xóa 3 chỉ mục thừa: các cột này đã có ràng buộc UNIQUE (tự tạo chỉ mục), chỉ mục riêng chỉ làm chậm thao tác ghi.
DROP INDEX `idx_employees_code` ON `employees`;
DROP INDEX `idx_accounts_username` ON `accounts`;
DROP INDEX `idx_suppliers_name` ON `suppliers`;
