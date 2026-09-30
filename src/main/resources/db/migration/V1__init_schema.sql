SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. BẢNG VAI TRÒ (ROLES)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. BẢNG HỒ SƠ NHÂN VIÊN (EMPLOYEES)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employees` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL,
    `dob` DATE NOT NULL,
    `gender` ENUM('Nam', 'Nữ', 'Giới tính khác') NOT NULL,
    `national_id` VARCHAR(12) NOT NULL UNIQUE,
    `phone` VARCHAR(15) NOT NULL UNIQUE,
    `address` VARCHAR(255) NULL,
    `hired_date` DATE NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. BẢNG TÀI KHOẢN (ACCOUNTS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `accounts` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` INT NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role_id` INT NOT NULL,
    `status` ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    `must_change_password` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_accounts_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_accounts_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. BẢNG LỊCH SỬ ĐĂNG NHẬP (LOGIN_HISTORY)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `login_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `account_id` INT NOT NULL,
    `login_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `result` ENUM('SUCCESS', 'FAILED') NOT NULL,
    `client_info` VARCHAR(255) NULL,
    CONSTRAINT `fk_login_history_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. DANH MỤC & THÔNG TIN THUỐC (MEDICINES)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `medicine_categories` (
     `id` INT AUTO_INCREMENT PRIMARY KEY,
     `name` VARCHAR(100) NOT NULL UNIQUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `medicines` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(30) NOT NULL UNIQUE,
    `name` VARCHAR(150) NOT NULL,
    `active_ingredient` VARCHAR(150) NULL,
    `unit` VARCHAR(20) NOT NULL,
    `category_id` INT NULL,
    `manufacturer` VARCHAR(150) NULL,
    `sell_price` DECIMAL(12, 2) NOT NULL,
    `requires_prescription` BOOLEAN NOT NULL DEFAULT FALSE,
    `min_stock_threshold` INT NOT NULL DEFAULT 0,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT `fk_medicines_categories` FOREIGN KEY (`category_id`) REFERENCES `medicine_categories` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. NHÀ CUNG CẤP & GIAO DỊCH KHO (INVENTORY TRANSACTIONS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `suppliers` (
   `id` INT AUTO_INCREMENT PRIMARY KEY,
   `name` VARCHAR(150) NOT NULL,
    `tax_code` VARCHAR(30) NULL UNIQUE,
    `address` VARCHAR(255) NULL,
    `contact_person` VARCHAR(100) NULL,
    `phone` VARCHAR(15) NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `inventory_transactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM('IMPORT', 'EXPORT_SALE', 'EXPORT_DISPOSAL', 'EXPORT_TRANSFER', 'RETURN_RESTOCK', 'RETURN_DESTROY') NOT NULL,
    `supplier_id` INT NULL,
    `employee_id` INT NOT NULL,
    `transaction_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `note` VARCHAR(255) NULL,
    CONSTRAINT `fk_transactions_suppliers` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. LÔ THUỐC VÀ HẠN DÙNG (INVENTORY LOTS - FEFO)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inventory_lots` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `medicine_id` INT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `lot_number` VARCHAR(50) NOT NULL,
    `expiry_date` DATE NOT NULL,
    `import_price` DECIMAL(12, 2) NOT NULL,
    `quantity_in` INT NOT NULL,
    `quantity_remaining` INT NOT NULL,
    CONSTRAINT `fk_inventory_lots_medicines` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_inventory_lots_transactions` FOREIGN KEY (`transaction_id`) REFERENCES `inventory_transactions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `uq_lot_medicine_transaction` UNIQUE (`transaction_id`, `medicine_id`, `lot_number`),
    INDEX `idx_inventory_lots_fefo` (`medicine_id`, `expiry_date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. HÓA ĐƠN BÁN LẺ (SALES ORDERS & ITEMS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sales_orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(30) NOT NULL UNIQUE,
    `employee_id` INT NOT NULL,
    `customer_phone` VARCHAR(15) NULL,
    `total_amount` DECIMAL(14, 2) NOT NULL,
    `discount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `payment_method` ENUM('CASH', 'TRANSFER', 'CARD') NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sales_orders_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sales_order_items` (
   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
   `sales_order_id` BIGINT NOT NULL,
   `medicine_id` INT NOT NULL,
   `inventory_lot_id` BIGINT NOT NULL,
   `quantity` INT NOT NULL,
   `unit_price` DECIMAL(12, 2) NOT NULL,
    `line_total` DECIMAL(14, 2) NOT NULL,
    CONSTRAINT `fk_so_items_sales_orders` FOREIGN KEY (`sales_order_id`) REFERENCES `sales_orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_so_items_medicines` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_so_items_inventory_lots` FOREIGN KEY (`inventory_lot_id`) REFERENCES `inventory_lots` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `uq_order_item_lot` UNIQUE (`sales_order_id`, `inventory_lot_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. PHIẾU TRẢ HÀNG & CHI TIẾT TRẢ (SALES RETURNS & ITEMS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sales_returns` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(30) NOT NULL UNIQUE,
    `sales_order_id` BIGINT NOT NULL,
    `employee_id` INT NOT NULL,
    `refund_amount` DECIMAL(14, 2) NOT NULL,
    `refund_method` ENUM('CASH', 'TRANSFER') NOT NULL DEFAULT 'CASH',
    `reason` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sales_returns_sales_orders` FOREIGN KEY (`sales_order_id`) REFERENCES `sales_orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_sales_returns_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_sales_returns_order` (`sales_order_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sales_return_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sales_return_id` BIGINT NOT NULL,
    `medicine_id` INT NOT NULL,
    `inventory_lot_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `refund_price` DECIMAL(12, 2) NOT NULL,
    `line_total` DECIMAL(14, 2) NOT NULL,
    `disposition` ENUM('RESTOCK', 'QUARANTINE_DESTROY') NOT NULL DEFAULT 'QUARANTINE_DESTROY',
    CONSTRAINT `fk_sri_returns` FOREIGN KEY (`sales_return_id`) REFERENCES `sales_returns` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_sri_medicines` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_sri_lots` FOREIGN KEY (`inventory_lot_id`) REFERENCES `inventory_lots` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `uq_return_item_lot` UNIQUE (`sales_return_id`, `inventory_lot_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `roles` (`code`, `name`) VALUES ('MANAGER', 'Quản lý cửa hàng'), ('STAFF', 'Nhân viên bán thuốc');

INSERT INTO `employees` (`full_name`, `dob`, `gender`, `national_id`, `address`, `phone`, `hired_date`, `status`) VALUES
    ('Quản lý cửa hàng', '1990-01-01', 'Nam', '001090000001', 'Hà Nội', '0985549854', '2026-01-01', 'ACTIVE');

INSERT INTO `accounts` (`employee_id`, `password_hash`, `role_id`, `status`, `must_change_password`) VALUES (
  (SELECT `id` FROM `employees` WHERE `national_id` = '001090000001'),
  '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce',
  (SELECT `id` FROM `roles` WHERE `code` = 'MANAGER'),
  'ACTIVE',FALSE
);

INSERT INTO `employees` (`full_name`, `dob`, `gender`, `national_id`, `address`, `phone`, `hired_date`, `status`) VALUES
    ('Nguyễn Quang Hiếu', '2005-03-10', 'Nam', '001095000002', 'Hà Nội', '0989261809', '2026-02-01', 'ACTIVE');

INSERT INTO `accounts` (`employee_id`, `password_hash`, `role_id`, `status`, `must_change_password`) VALUES (
  (SELECT `id` FROM `employees` WHERE `national_id` = '001095000002'),
  '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce',
  (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'),
  'ACTIVE', TRUE
);