-- 1. Chèn 5 nhân viên mới
INSERT INTO `employees` (`full_name`, `dob`, `gender`, `national_id`, `address`, `phone`, `hired_date`, `status`) VALUES
('Trần Thu Trang', '1998-05-15', 'Nữ', '001198000003', 'Cầu Giấy, Hà Nội', '0981112233', '2026-03-01', 'ACTIVE'),
('Phạm Quốc Đạt', '1996-08-20', 'Nam', '001096000004', 'Đống Đa, Hà Nội', '0982223344', '2026-03-01', 'ACTIVE'),
('Lê Thị Mai Anh', '2001-11-12', 'Nữ', '001201000005', 'Thanh Xuân, Hà Nội', '0983334455', '2026-04-15', 'ACTIVE'),
('Vũ Hoàng Long', '1999-02-28', 'Nam', '001099000006', 'Hai Bà Trưng, Hà Nội', '0984445566', '2026-05-01', 'ACTIVE'),
('Đỗ Thảo Linh', '2002-09-05', 'Nữ', '001202000007', 'Ba Đình, Hà Nội', '0985556677', '2026-06-01', 'ACTIVE');

-- 2. Cấp tài khoản tương ứng (Đã bỏ username, đăng nhập bằng SĐT/CCCD, mật khẩu mặc định: 123456)
INSERT INTO `accounts` (`employee_id`, `password_hash`, `role_id`, `status`, `must_change_password`) VALUES
((SELECT `id` FROM `employees` WHERE `national_id` = '001198000003'), '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce', (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'), 'ACTIVE', TRUE),
((SELECT `id` FROM `employees` WHERE `national_id` = '001096000004'), '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce', (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'), 'ACTIVE', TRUE),
((SELECT `id` FROM `employees` WHERE `national_id` = '001201000005'), '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce', (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'), 'ACTIVE', TRUE),
((SELECT `id` FROM `employees` WHERE `national_id` = '001099000006'), '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce', (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'), 'ACTIVE', TRUE),
((SELECT `id` FROM `employees` WHERE `national_id` = '001202000007'), '$2a$12$1/E8j01kLrkJvjJ4.R91x.5A7B8nzOtVk1mZSonKBWP/Ecm3Abtce', (SELECT `id` FROM `roles` WHERE `code` = 'STAFF'), 'ACTIVE', TRUE);