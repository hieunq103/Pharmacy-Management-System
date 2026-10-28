-- Bộ đếm cấp mã nghiệp vụ (mã nhân viên PMSxxxx). Mỗi lần cấp mã khóa dòng bộ đếm trong cùng giao dịch
-- với việc thêm bản ghi nên nhiều người cùng thêm không bao giờ nhận trùng mã.
CREATE TABLE IF NOT EXISTS `code_sequences` (
    `name` VARCHAR(30) NOT NULL PRIMARY KEY,
    `current_number` BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `code_sequences` (`name`, `current_number`)
SELECT 'EMPLOYEE', COALESCE(MAX(CAST(SUBSTRING(`code`, 4) AS UNSIGNED)), 0)
FROM `employees` WHERE `code` REGEXP '^PMS[0-9]+$';
