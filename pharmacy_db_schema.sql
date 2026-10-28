-- =============================================================================
-- CSDL HỆ THỐNG QUẢN LÝ NHÀ THUỐC (PHARMAOS) - FINAL CONSOLIDATED SCHEMA (V1 - V5)
-- Dùng để import trực tiếp vào MySQL / MySQL Workbench để Reverse Engineer sinh mô hình ERD
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `pharmacy_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `pharmacy_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. BẢNG VAI TRÒ (ROLES)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. BẢNG HỒ SƠ NHÂN VIÊN (EMPLOYEES)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `employees`;
CREATE TABLE `employees` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `full_name` VARCHAR(100) NOT NULL,
    `dob` DATE NOT NULL,
    `gender` ENUM('Nam', 'Nữ', 'Khác') NOT NULL,
    `national_id` VARCHAR(12) NOT NULL UNIQUE,
    `phone` VARCHAR(15) NOT NULL UNIQUE,
    `address` VARCHAR(255) NULL,
    `hired_date` DATE NULL,
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_employees_code` (`code`),
    INDEX `idx_employees_full_name` (`full_name`),
    INDEX `idx_employees_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. BẢNG TÀI KHOẢN (ACCOUNTS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` INT NOT NULL UNIQUE,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role_id` INT NOT NULL,
    `status` ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    `must_change_password` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_accounts_username` (`username`),
    CONSTRAINT `fk_accounts_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_accounts_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. BẢNG LỊCH SỬ ĐĂNG NHẬP (LOGIN_HISTORY)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `login_history`;
CREATE TABLE `login_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `account_id` INT NOT NULL,
    `login_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `result` ENUM('SUCCESS', 'FAILED') NOT NULL,
    `client_info` VARCHAR(255) NULL,
    INDEX `idx_login_history_account_time` (`account_id`, `login_at`),
    CONSTRAINT `fk_login_history_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. DANH MỤC THUỐC (MEDICINE_CATEGORIES)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `medicine_categories`;
CREATE TABLE `medicine_categories` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. THÔNG TIN THUỐC (MEDICINES)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `medicines`;
CREATE TABLE `medicines` (
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
    INDEX `idx_medicines_name` (`name`),
    INDEX `idx_medicines_active_ingredient` (`active_ingredient`),
    INDEX `idx_medicines_status_category` (`status`, `category_id`),
    CONSTRAINT `fk_medicines_categories` FOREIGN KEY (`category_id`) REFERENCES `medicine_categories` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `chk_medicines_values` CHECK (`sell_price` >= 0 AND `min_stock_threshold` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. NHÀ CUNG CẤP (SUPPLIERS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `suppliers`;
CREATE TABLE `suppliers` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `tax_code` VARCHAR(30) NULL UNIQUE,
    `address` VARCHAR(255) NULL,
    `contact_person` VARCHAR(100) NULL,
    `phone` VARCHAR(15) NULL,
    INDEX `idx_suppliers_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. HÓA ĐƠN BÁN LẺ (SALES_ORDERS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sales_orders`;
CREATE TABLE `sales_orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(30) NOT NULL UNIQUE,
    `employee_id` INT NOT NULL,
    `customer_phone` VARCHAR(15) NULL,
    `total_amount` DECIMAL(14, 2) NOT NULL,
    `discount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `payment_method` ENUM('CASH', 'TRANSFER', 'CARD') NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sales_orders_phone` (`customer_phone`),
    INDEX `idx_sales_orders_created_at` (`created_at`),
    INDEX `idx_sales_orders_employee_created` (`employee_id`, `created_at`),
    CONSTRAINT `fk_sales_orders_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_sales_orders_amounts` CHECK (`total_amount` >= 0 AND `discount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. GIAO DỊCH KHO (INVENTORY_TRANSACTIONS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `inventory_transactions`;
CREATE TABLE `inventory_transactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` ENUM('IMPORT', 'EXPORT_SALE', 'EXPORT_DISPOSAL') NOT NULL,
    `supplier_id` INT NULL,
    `sales_order_id` BIGINT NULL,
    `employee_id` INT NOT NULL,
    `transaction_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `note` VARCHAR(255) NULL,
    INDEX `idx_transactions_at` (`transaction_at`),
    INDEX `idx_transactions_type_at` (`type`, `transaction_at`),
    UNIQUE INDEX `uq_transactions_sales_order` (`sales_order_id`),
    CONSTRAINT `fk_transactions_suppliers` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_transactions_sales_orders` FOREIGN KEY (`sales_order_id`) REFERENCES `sales_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_transactions_employees` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_transactions_sale_link` CHECK ((`type` = 'EXPORT_SALE') = (`sales_order_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 10. LÔ THUỐC VÀ HẠN DÙNG (INVENTORY_LOTS - FEFO)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `inventory_lots`;
CREATE TABLE `inventory_lots` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `medicine_id` INT NOT NULL,
    `transaction_id` BIGINT NOT NULL,
    `lot_number` VARCHAR(50) NOT NULL,
    `expiry_date` DATE NOT NULL,
    `import_price` DECIMAL(12, 2) NOT NULL,
    `quantity_in` INT NOT NULL,
    `quantity_remaining` INT NOT NULL,
    UNIQUE INDEX `uq_lot_medicine_transaction` (`transaction_id`, `medicine_id`, `lot_number`),
    UNIQUE INDEX `uq_inventory_lots_id_medicine` (`id`, `medicine_id`),
    INDEX `idx_inventory_lots_fefo` (`medicine_id`, `expiry_date`),
    INDEX `idx_inventory_lots_stock_fefo` (`medicine_id`, `quantity_remaining`, `expiry_date`),
    INDEX `idx_inventory_lots_expiry` (`expiry_date`, `quantity_remaining`),
    CONSTRAINT `fk_inventory_lots_medicines` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_inventory_lots_transactions` FOREIGN KEY (`transaction_id`) REFERENCES `inventory_transactions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_inventory_lots_quantity` CHECK (`quantity_in` > 0 AND `quantity_remaining` >= 0),
    CONSTRAINT `chk_inventory_lots_price` CHECK (`import_price` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 11. LÔ THUỐC ĐEM ĐI HỦY (INVENTORY_DISPOSAL_ITEMS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `inventory_disposal_items`;
CREATE TABLE `inventory_disposal_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `transaction_id` BIGINT NOT NULL,
    `inventory_lot_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `reason` VARCHAR(255) NULL,
    CONSTRAINT `fk_disposal_items_transactions` FOREIGN KEY (`transaction_id`) REFERENCES `inventory_transactions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_disposal_items_lots` FOREIGN KEY (`inventory_lot_id`) REFERENCES `inventory_lots` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_disposal_items_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 12. CHI TIẾT HÓA ĐƠN BÁN LẺ (SALES_ORDER_ITEMS)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sales_order_items`;
CREATE TABLE `sales_order_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sales_order_id` BIGINT NOT NULL,
    `medicine_id` INT NOT NULL,
    `inventory_lot_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL,
    `unit_price` DECIMAL(12, 2) NOT NULL,
    `line_total` DECIMAL(14, 2) NOT NULL,
    UNIQUE INDEX `uq_order_item_lot` (`sales_order_id`, `inventory_lot_id`),
    INDEX `idx_so_items_order_cover` (`sales_order_id`, `medicine_id`, `quantity`, `line_total`),
    CONSTRAINT `fk_so_items_sales_orders` FOREIGN KEY (`sales_order_id`) REFERENCES `sales_orders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_so_items_medicines` FOREIGN KEY (`medicine_id`) REFERENCES `medicines` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_so_items_lot_medicine` FOREIGN KEY (`inventory_lot_id`, `medicine_id`) REFERENCES `inventory_lots` (`id`, `medicine_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_so_items_values` CHECK (`quantity` > 0 AND `unit_price` >= 0 AND `line_total` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
