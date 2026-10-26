-- =============================================================================
-- V5: Ràng buộc toàn vẹn, chỉ mục cho truy vấn báo cáo/thống kê, cờ đổi mật khẩu lần đầu
-- Yêu cầu MySQL >= 8.0.16 (CHECK constraint) và >= 8.0.14 (LATERAL dùng trong repository).
-- Không sửa V1–V4 (Flyway sẽ báo lỗi checksum trên các CSDL đã migrate).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TÀI KHOẢN: bắt buộc đổi mật khẩu ở lần đăng nhập đầu
-- -----------------------------------------------------------------------------
ALTER TABLE `accounts`
    ADD COLUMN `must_change_password` BOOLEAN NOT NULL DEFAULT FALSE AFTER `status`;

-- -----------------------------------------------------------------------------
-- 2. LỊCH SỬ ĐĂNG NHẬP: xem lịch sử theo tài khoản, sắp xếp theo thời gian (ORDER BY login_at DESC LIMIT 100)
--    (index (account_id, login_at) thay thế index FK chỉ có account_id)
-- -----------------------------------------------------------------------------
ALTER TABLE `login_history`
    ADD INDEX `idx_login_history_account_time` (`account_id`, `login_at`),
    DROP INDEX `fk_login_history_accounts`;

-- -----------------------------------------------------------------------------
-- 3. HÓA ĐƠN: báo cáo theo nhân viên lọc employee_id + khoảng created_at
-- -----------------------------------------------------------------------------
ALTER TABLE `sales_orders`
    ADD INDEX `idx_sales_orders_employee_created` (`employee_id`, `created_at`),
    DROP INDEX `fk_sales_orders_employees`,
    ADD CONSTRAINT `chk_sales_orders_amounts` CHECK (`total_amount` >= 0 AND `discount` >= 0);

-- -----------------------------------------------------------------------------
-- 4. LÔ THUỐC
--    - idx_inventory_lots_expiry: cảnh báo lô cận/hết hạn (WHERE expiry_date <= ? ORDER BY expiry_date LIMIT n)
--    - uq_inventory_lots_id_medicine: đích của FK tổ hợp từ sales_order_items (mục 5)
--    - CHECK số lượng/giá không âm
-- -----------------------------------------------------------------------------
ALTER TABLE `inventory_lots`
    ADD INDEX `idx_inventory_lots_expiry` (`expiry_date`, `quantity_remaining`),
    ADD UNIQUE INDEX `uq_inventory_lots_id_medicine` (`id`, `medicine_id`),
    ADD CONSTRAINT `chk_inventory_lots_quantity` CHECK (`quantity_in` > 0 AND `quantity_remaining` >= 0),
    ADD CONSTRAINT `chk_inventory_lots_price` CHECK (`import_price` >= 0);

-- -----------------------------------------------------------------------------
-- 5. CHI TIẾT HÓA ĐƠN
--    - Chỉ mục phủ cho các truy vấn tổng hợp theo đơn (SUM quantity/line_total, COUNT DISTINCT medicine_id)
--    - medicine_id là cột phi chuẩn hóa (đã có qua inventory_lots); FK tổ hợp đảm bảo nó luôn khớp với lô đã bán
-- -----------------------------------------------------------------------------
ALTER TABLE `sales_order_items`
    ADD INDEX `idx_so_items_order_cover` (`sales_order_id`, `medicine_id`, `quantity`, `line_total`),
    ADD CONSTRAINT `fk_so_items_lot_medicine` FOREIGN KEY (`inventory_lot_id`, `medicine_id`)
        REFERENCES `inventory_lots` (`id`, `medicine_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `chk_so_items_values` CHECK (`quantity` > 0 AND `unit_price` >= 0 AND `line_total` >= 0);

-- -----------------------------------------------------------------------------
-- 6. GIAO DỊCH KHO
--    - (type, transaction_at): lịch sử nhập/xuất lọc theo loại + khoảng ngày (thay index đơn trên type)
--    - Mỗi hóa đơn chỉ sinh đúng 1 giao dịch EXPORT_SALE (UNIQUE sales_order_id)
--    - EXPORT_SALE bắt buộc có sales_order_id, các loại khác thì không.
--      MySQL không cho CHECK trên cột FK có ON UPDATE CASCADE -> tạo lại FK với RESTRICT
--      (khóa chính AUTO_INCREMENT không bao giờ bị cập nhật nên không mất tính năng).
-- -----------------------------------------------------------------------------
ALTER TABLE `inventory_transactions`
    DROP FOREIGN KEY `fk_transactions_sales_orders`;

ALTER TABLE `inventory_transactions`
    ADD INDEX `idx_transactions_type_at` (`type`, `transaction_at`),
    DROP INDEX `idx_transactions_type`,
    ADD UNIQUE INDEX `uq_transactions_sales_order` (`sales_order_id`),
    DROP INDEX `fk_transactions_sales_orders`,
    ADD CONSTRAINT `fk_transactions_sales_orders` FOREIGN KEY (`sales_order_id`)
        REFERENCES `sales_orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `chk_transactions_sale_link` CHECK ((`type` = 'EXPORT_SALE') = (`sales_order_id` IS NOT NULL));

-- -----------------------------------------------------------------------------
-- 7. CHI TIẾT XUẤT HỦY: V1 tạo bảng thiếu khai báo charset/collation (cột reason tiếng Việt)
-- -----------------------------------------------------------------------------
ALTER TABLE `inventory_disposal_items`
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `inventory_disposal_items`
    ADD CONSTRAINT `chk_disposal_items_quantity` CHECK (`quantity` > 0);

-- -----------------------------------------------------------------------------
-- 8. DANH MỤC
--    - suppliers.name: tra cứu "tìm hoặc tạo nhà cung cấp" khi nhập kho
--    - Bỏ FULLTEXT ft_medicines_search: không truy vấn nào dùng MATCH ... AGAINST (tìm kiếm dùng LIKE),
--      chỉ làm chậm INSERT/UPDATE medicines
-- -----------------------------------------------------------------------------
ALTER TABLE `suppliers`
    ADD INDEX `idx_suppliers_name` (`name`);

ALTER TABLE `medicines`
    DROP INDEX `ft_medicines_search`,
    ADD CONSTRAINT `chk_medicines_values` CHECK (`sell_price` >= 0 AND `min_stock_threshold` >= 0);
