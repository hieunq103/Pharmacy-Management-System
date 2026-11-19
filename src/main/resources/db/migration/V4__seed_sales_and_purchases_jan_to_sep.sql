SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-01-04 08:30:00', 'Nhập hàng định kỳ Tháng 01/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202601-01-MED001', '2028-05-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202601-01-MED002', '2028-05-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202601-01-MED003', '2028-05-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202601-01-MED004', '2028-05-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202601-01-MED008', '2028-05-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202601-01-MED021', '2028-05-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202601-01-MED022', '2028-05-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202601-01-MED024', '2028-05-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202601-01-MED026', '2028-05-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202601-01-MED027', '2028-05-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-01-18 09:15:00', 'Nhập hàng định kỳ Tháng 01/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202601-02-MED041', '2029-07-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202601-02-MED043', '2029-07-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202601-02-MED045', '2029-07-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202601-02-MED054', '2029-07-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202601-02-MED061', '2029-07-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202601-02-MED066', '2029-07-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202601-02-MED071', '2029-07-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202601-02-MED073', '2029-07-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202601-02-MED145', '2029-07-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202601-02-MED146', '2029-07-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-02-04 08:30:00', 'Nhập hàng định kỳ Tháng 02/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202602-01-MED001', '2028-06-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202602-01-MED002', '2028-06-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202602-01-MED003', '2028-06-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202602-01-MED004', '2028-06-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202602-01-MED008', '2028-06-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202602-01-MED021', '2028-06-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202602-01-MED022', '2028-06-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202602-01-MED024', '2028-06-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202602-01-MED026', '2028-06-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202602-01-MED027', '2028-06-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-02-18 09:15:00', 'Nhập hàng định kỳ Tháng 02/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202602-02-MED041', '2029-08-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202602-02-MED043', '2029-08-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202602-02-MED045', '2029-08-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202602-02-MED054', '2029-08-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202602-02-MED061', '2029-08-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202602-02-MED066', '2029-08-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202602-02-MED071', '2029-08-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202602-02-MED073', '2029-08-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202602-02-MED145', '2029-08-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202602-02-MED146', '2029-08-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-03-04 08:30:00', 'Nhập hàng định kỳ Tháng 03/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202603-01-MED001', '2028-07-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202603-01-MED002', '2028-07-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202603-01-MED003', '2028-07-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202603-01-MED004', '2028-07-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202603-01-MED008', '2028-07-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202603-01-MED021', '2028-07-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202603-01-MED022', '2028-07-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202603-01-MED024', '2028-07-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202603-01-MED026', '2028-07-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202603-01-MED027', '2028-07-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-03-18 09:15:00', 'Nhập hàng định kỳ Tháng 03/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202603-02-MED041', '2029-09-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202603-02-MED043', '2029-09-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202603-02-MED045', '2029-09-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202603-02-MED054', '2029-09-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202603-02-MED061', '2029-09-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202603-02-MED066', '2029-09-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202603-02-MED071', '2029-09-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202603-02-MED073', '2029-09-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202603-02-MED145', '2029-09-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202603-02-MED146', '2029-09-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-04-04 08:30:00', 'Nhập hàng định kỳ Tháng 04/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202604-01-MED001', '2028-08-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202604-01-MED002', '2028-08-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202604-01-MED003', '2028-08-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202604-01-MED004', '2028-08-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202604-01-MED008', '2028-08-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202604-01-MED021', '2028-08-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202604-01-MED022', '2028-08-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202604-01-MED024', '2028-08-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202604-01-MED026', '2028-08-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202604-01-MED027', '2028-08-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-04-18 09:15:00', 'Nhập hàng định kỳ Tháng 04/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202604-02-MED041', '2029-10-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202604-02-MED043', '2029-10-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202604-02-MED045', '2029-10-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202604-02-MED054', '2029-10-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202604-02-MED061', '2029-10-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202604-02-MED066', '2029-10-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202604-02-MED071', '2029-10-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202604-02-MED073', '2029-10-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202604-02-MED145', '2029-10-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202604-02-MED146', '2029-10-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-05-04 08:30:00', 'Nhập hàng định kỳ Tháng 05/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202605-01-MED001', '2028-09-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202605-01-MED002', '2028-09-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202605-01-MED003', '2028-09-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202605-01-MED004', '2028-09-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202605-01-MED008', '2028-09-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202605-01-MED021', '2028-09-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202605-01-MED022', '2028-09-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202605-01-MED024', '2028-09-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202605-01-MED026', '2028-09-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202605-01-MED027', '2028-09-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-05-18 09:15:00', 'Nhập hàng định kỳ Tháng 05/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202605-02-MED041', '2029-11-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202605-02-MED043', '2029-11-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202605-02-MED045', '2029-11-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202605-02-MED054', '2029-11-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202605-02-MED061', '2029-11-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202605-02-MED066', '2029-11-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202605-02-MED071', '2029-11-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202605-02-MED073', '2029-11-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202605-02-MED145', '2029-11-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202605-02-MED146', '2029-11-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-06-04 08:30:00', 'Nhập hàng định kỳ Tháng 06/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202606-01-MED001', '2028-10-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202606-01-MED002', '2028-10-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202606-01-MED003', '2028-10-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202606-01-MED004', '2028-10-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202606-01-MED008', '2028-10-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202606-01-MED021', '2028-10-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202606-01-MED022', '2028-10-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202606-01-MED024', '2028-10-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202606-01-MED026', '2028-10-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202606-01-MED027', '2028-10-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-06-18 09:15:00', 'Nhập hàng định kỳ Tháng 06/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202606-02-MED041', '2029-12-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202606-02-MED043', '2029-12-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202606-02-MED045', '2029-12-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202606-02-MED054', '2029-12-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202606-02-MED061', '2029-12-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202606-02-MED066', '2029-12-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202606-02-MED071', '2029-12-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202606-02-MED073', '2029-12-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202606-02-MED145', '2029-12-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202606-02-MED146', '2029-12-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-07-04 08:30:00', 'Nhập hàng định kỳ Tháng 07/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202607-01-MED001', '2028-11-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202607-01-MED002', '2028-11-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202607-01-MED003', '2028-11-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202607-01-MED004', '2028-11-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202607-01-MED008', '2028-11-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202607-01-MED021', '2028-11-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202607-01-MED022', '2028-11-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202607-01-MED024', '2028-11-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202607-01-MED026', '2028-11-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202607-01-MED027', '2028-11-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-07-18 09:15:00', 'Nhập hàng định kỳ Tháng 07/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202607-02-MED041', '2029-01-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202607-02-MED043', '2029-01-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202607-02-MED045', '2029-01-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202607-02-MED054', '2029-01-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202607-02-MED061', '2029-01-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202607-02-MED066', '2029-01-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202607-02-MED071', '2029-01-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202607-02-MED073', '2029-01-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202607-02-MED145', '2029-01-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202607-02-MED146', '2029-01-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-08-04 08:30:00', 'Nhập hàng định kỳ Tháng 08/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202608-01-MED001', '2028-12-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202608-01-MED002', '2028-12-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202608-01-MED003', '2028-12-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202608-01-MED004', '2028-12-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202608-01-MED008', '2028-12-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202608-01-MED021', '2028-12-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202608-01-MED022', '2028-12-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202608-01-MED024', '2028-12-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202608-01-MED026', '2028-12-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202608-01-MED027', '2028-12-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-08-18 09:15:00', 'Nhập hàng định kỳ Tháng 08/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202608-02-MED041', '2029-02-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202608-02-MED043', '2029-02-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202608-02-MED045', '2029-02-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202608-02-MED054', '2029-02-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202608-02-MED061', '2029-02-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202608-02-MED066', '2029-02-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202608-02-MED071', '2029-02-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202608-02-MED073', '2029-02-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202608-02-MED145', '2029-02-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202608-02-MED146', '2029-02-25', 63800.00, 250, 200);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 1, 1, '2026-09-04 08:30:00', 'Nhập hàng định kỳ Tháng 09/2026 - Đợt 1 từ DHG Pharma');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), @tx_id, 'LOT-202609-01-MED001', '2028-01-28', 108800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), @tx_id, 'LOT-202609-01-MED002', '2028-01-28', 157500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), @tx_id, 'LOT-202609-01-MED003', '2028-01-28', 9000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), @tx_id, 'LOT-202609-01-MED004', '2028-01-28', 285000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), @tx_id, 'LOT-202609-01-MED008', '2028-01-28', 63800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), @tx_id, 'LOT-202609-01-MED021', '2028-01-28', 138800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), @tx_id, 'LOT-202609-01-MED022', '2028-01-28', 105000.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), @tx_id, 'LOT-202609-01-MED024', '2028-01-28', 48800.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), @tx_id, 'LOT-202609-01-MED026', '2028-01-28', 73500.00, 300, 240),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), @tx_id, 'LOT-202609-01-MED027', '2028-01-28', 28500.00, 300, 240);

INSERT INTO `inventory_transactions` (`type`, `supplier_id`, `employee_id`, `transaction_at`, `note`) VALUES
('IMPORT', 4, 1, '2026-09-18 09:15:00', 'Nhập hàng định kỳ Tháng 09/2026 - Đợt 2 từ Sanofi-Aventis');
SET @tx_id = LAST_INSERT_ID();

INSERT INTO `inventory_lots` (`medicine_id`, `transaction_id`, `lot_number`, `expiry_date`, `import_price`, `quantity_in`, `quantity_remaining`) VALUES
((SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), @tx_id, 'LOT-202609-02-MED041', '2029-03-25', 12000.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), @tx_id, 'LOT-202609-02-MED043', '2029-03-25', 101200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), @tx_id, 'LOT-202609-02-MED045', '2029-03-25', 58500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), @tx_id, 'LOT-202609-02-MED054', '2029-03-25', 56200.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), @tx_id, 'LOT-202609-02-MED061', '2029-03-25', 273800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), @tx_id, 'LOT-202609-02-MED066', '2029-03-25', 123800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), @tx_id, 'LOT-202609-02-MED071', '2029-03-25', 97500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), @tx_id, 'LOT-202609-02-MED073', '2029-03-25', 138800.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), @tx_id, 'LOT-202609-02-MED145', '2029-03-25', 73500.00, 250, 200),
((SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), @tx_id, 'LOT-202609-02-MED146', '2029-03-25', 63800.00, 250, 200);

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260102-093000-11', 1, '0978998877', 605000.00, 0.00, 'CASH', '2026-01-02 09:30:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-01-02 09:30:00', 'Bán lẻ tại quầy - Mã đơn HDB-260102-093000-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260105-084900-12', 1, '0989261809', 365000.00, 0.00, 'CASH', '2026-01-05 08:49:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 1, 365000.00, 365000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-01-05 08:49:00', 'Bán lẻ tại quầy - Mã đơn HDB-260105-084900-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260108-204100-13', 2, '0978998877', 355000.00, 0.00, 'TRANSFER', '2026-01-08 20:41:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 2, 135000.00, 270000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-08 20:41:00', 'Bán lẻ tại quầy - Mã đơn HDB-260108-204100-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260111-151500-14', 2, '0934556677', 196000.00, 0.00, 'TRANSFER', '2026-01-11 15:15:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-11 15:15:00', 'Bán lẻ tại quầy - Mã đơn HDB-260111-151500-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260114-085500-15', 2, '0966882244', 102000.00, 0.00, 'TRANSFER', '2026-01-14 08:55:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 2, 12000.00, 24000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 1, 78000.00, 78000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-14 08:55:00', 'Bán lẻ tại quầy - Mã đơn HDB-260114-085500-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260117-164200-16', 2, '0988554433', 248000.00, 0.00, 'CASH', '2026-01-17 16:42:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-17 16:42:00', 'Bán lẻ tại quầy - Mã đơn HDB-260117-164200-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260120-192400-17', 1, '0912345678', 695000.00, 0.00, 'CASH', '2026-01-20 19:24:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 1, 380000.00, 380000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 1, 165000.00, 165000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-01-20 19:24:00', 'Bán lẻ tại quầy - Mã đơn HDB-260120-192400-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260123-095000-18', 2, '0918776655', 526000.00, 0.00, 'CASH', '2026-01-23 09:50:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 2, 78000.00, 156000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-23 09:50:00', 'Bán lẻ tại quầy - Mã đơn HDB-260123-095000-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260126-163700-19', 1, '0934556677', 210000.00, 0.00, 'CASH', '2026-01-26 16:37:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 1, 210000.00, 210000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-01-26 16:37:00', 'Bán lẻ tại quầy - Mã đơn HDB-260126-163700-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260128-204800-20', 2, '0918776655', 196000.00, 0.00, 'CASH', '2026-01-28 20:48:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-01-28 20:48:00', 'Bán lẻ tại quầy - Mã đơn HDB-260128-204800-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260202-174200-11', 2, '0988554433', 490000.00, 0.00, 'CASH', '2026-02-02 17:42:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 2, 210000.00, 420000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-02 17:42:00', 'Bán lẻ tại quầy - Mã đơn HDB-260202-174200-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260205-182600-12', 2, '0903112233', 452000.00, 0.00, 'TRANSFER', '2026-02-05 18:26:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 2, 165000.00, 330000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 2, 12000.00, 24000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-05 18:26:00', 'Bán lẻ tại quầy - Mã đơn HDB-260205-182600-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260208-113600-13', 1, '0918776655', 290000.00, 0.00, 'CASH', '2026-02-08 11:36:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 2, 145000.00, 290000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-02-08 11:36:00', 'Bán lẻ tại quầy - Mã đơn HDB-260208-113600-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260211-194400-14', 1, '0978998877', 85000.00, 0.00, 'TRANSFER', '2026-02-11 19:44:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-02-11 19:44:00', 'Bán lẻ tại quầy - Mã đơn HDB-260211-194400-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260214-182700-15', 2, '0903112233', 281000.00, 0.00, 'CASH', '2026-02-14 18:27:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-14 18:27:00', 'Bán lẻ tại quầy - Mã đơn HDB-260214-182700-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260217-182400-16', 2, '0903112233', 310000.00, 0.00, 'CASH', '2026-02-17 18:24:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 2, 130000.00, 260000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 1, 12000.00, 12000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-17 18:24:00', 'Bán lẻ tại quầy - Mã đơn HDB-260217-182400-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260220-192900-17', 2, '0988554433', 441000.00, 0.00, 'TRANSFER', '2026-02-20 19:29:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 1, 140000.00, 140000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 2, 78000.00, 156000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-20 19:29:00', 'Bán lẻ tại quầy - Mã đơn HDB-260220-192900-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260223-083200-18', 2, '0912345678', 400000.00, 0.00, 'CASH', '2026-02-23 08:32:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 2, 165000.00, 330000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-02-23 08:32:00', 'Bán lẻ tại quầy - Mã đơn HDB-260223-083200-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260226-173900-19', 1, '0903112233', 371000.00, 0.00, 'CASH', '2026-02-26 17:39:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 2, 140000.00, 280000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 1, 75000.00, 75000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 1, 16000.00, 16000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-02-26 17:39:00', 'Bán lẻ tại quầy - Mã đơn HDB-260226-173900-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260228-183500-20', 1, '0978998877', 185000.00, 0.00, 'TRANSFER', '2026-02-28 18:35:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-02-28 18:35:00', 'Bán lẻ tại quầy - Mã đơn HDB-260228-183500-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260302-175100-11', 1, '0918776655', 130000.00, 0.00, 'CASH', '2026-03-02 17:51:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 1, 130000.00, 130000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-02 17:51:00', 'Bán lẻ tại quầy - Mã đơn HDB-260302-175100-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260305-181800-12', 1, '0903112233', 335000.00, 0.00, 'TRANSFER', '2026-03-05 18:18:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-05 18:18:00', 'Bán lẻ tại quầy - Mã đơn HDB-260305-181800-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260308-162900-13', 1, '0966882244', 1060000.00, 0.00, 'TRANSFER', '2026-03-08 16:29:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 2, 365000.00, 730000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 2, 165000.00, 330000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-08 16:29:00', 'Bán lẻ tại quầy - Mã đơn HDB-260308-162900-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260311-172100-14', 2, '0918776655', 390000.00, 0.00, 'TRANSFER', '2026-03-11 17:21:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-03-11 17:21:00', 'Bán lẻ tại quầy - Mã đơn HDB-260311-172100-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260314-141600-15', 1, '0978998877', 1095000.00, 0.00, 'CASH', '2026-03-14 14:16:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 3, 365000.00, 1095000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-14 14:16:00', 'Bán lẻ tại quầy - Mã đơn HDB-260314-141600-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260317-153600-16', 1, '0912345678', 405000.00, 0.00, 'CASH', '2026-03-17 15:36:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 3, 135000.00, 405000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-17 15:36:00', 'Bán lẻ tại quầy - Mã đơn HDB-260317-153600-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260320-204600-17', 1, '0989261809', 1084000.00, 0.00, 'CASH', '2026-03-20 20:46:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 3, 98000.00, 294000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 3, 140000.00, 420000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-20 20:46:00', 'Bán lẻ tại quầy - Mã đơn HDB-260320-204600-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260323-145200-18', 1, '0989261809', 75000.00, 0.00, 'CASH', '2026-03-23 14:52:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 1, 75000.00, 75000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-23 14:52:00', 'Bán lẻ tại quầy - Mã đơn HDB-260323-145200-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260326-095300-19', 1, '0966882244', 1166000.00, 0.00, 'TRANSFER', '2026-03-26 09:53:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 2, 380000.00, 760000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 1, 16000.00, 16000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-26 09:53:00', 'Bán lẻ tại quầy - Mã đơn HDB-260326-095300-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260328-143000-20', 1, '0934556677', 290000.00, 0.00, 'CASH', '2026-03-28 14:30:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 2, 145000.00, 290000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-03-28 14:30:00', 'Bán lẻ tại quầy - Mã đơn HDB-260328-143000-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260402-143600-11', 1, '0912345678', 290000.00, 0.00, 'CASH', '2026-04-02 14:36:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 2, 145000.00, 290000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-04-02 14:36:00', 'Bán lẻ tại quầy - Mã đơn HDB-260402-143600-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260405-091600-12', 2, '0966882244', 878000.00, 0.00, 'TRANSFER', '2026-04-05 09:16:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 2, 85000.00, 170000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 3, 210000.00, 630000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 1, 78000.00, 78000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-05 09:16:00', 'Bán lẻ tại quầy - Mã đơn HDB-260405-091600-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260408-193900-13', 2, '0989261809', 388000.00, 0.00, 'CASH', '2026-04-08 19:39:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 1, 78000.00, 78000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 3, 38000.00, 114000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-08 19:39:00', 'Bán lẻ tại quầy - Mã đơn HDB-260408-193900-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260411-161100-14', 2, '0966882244', 949000.00, 0.00, 'CASH', '2026-04-11 16:11:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 2, 380000.00, 760000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 3, 38000.00, 114000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 1, 75000.00, 75000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-11 16:11:00', 'Bán lẻ tại quầy - Mã đơn HDB-260411-161100-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260414-183100-15', 1, '0912345678', 85000.00, 0.00, 'CASH', '2026-04-14 18:31:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-04-14 18:31:00', 'Bán lẻ tại quầy - Mã đơn HDB-260414-183100-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260417-091200-16', 1, '0966882244', 409000.00, 0.00, 'CASH', '2026-04-17 09:12:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 3, 38000.00, 114000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-04-17 09:12:00', 'Bán lẻ tại quầy - Mã đơn HDB-260417-091200-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260420-163400-17', 2, '0978998877', 835000.00, 0.00, 'TRANSFER', '2026-04-20 16:34:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 3, 65000.00, 195000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-20 16:34:00', 'Bán lẻ tại quầy - Mã đơn HDB-260420-163400-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260423-185300-18', 2, '0934556677', 145000.00, 0.00, 'TRANSFER', '2026-04-23 18:53:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-23 18:53:00', 'Bán lẻ tại quầy - Mã đơn HDB-260423-185300-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260426-144400-19', 2, '0988554433', 183000.00, 0.00, 'CASH', '2026-04-26 14:44:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-26 14:44:00', 'Bán lẻ tại quầy - Mã đơn HDB-260426-144400-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260428-181200-20', 2, '0966882244', 548000.00, 0.00, 'CASH', '2026-04-28 18:12:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 3, 16000.00, 48000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 1, 210000.00, 210000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 2, 145000.00, 290000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-04-28 18:12:00', 'Bán lẻ tại quầy - Mã đơn HDB-260428-181200-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260502-082400-11', 1, '0988554433', 468000.00, 0.00, 'CASH', '2026-05-02 08:24:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-02 08:24:00', 'Bán lẻ tại quầy - Mã đơn HDB-260502-082400-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260505-153200-12', 2, '0918776655', 492000.00, 0.00, 'CASH', '2026-05-05 15:32:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 3, 38000.00, 114000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 2, 140000.00, 280000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-05-05 15:32:00', 'Bán lẻ tại quầy - Mã đơn HDB-260505-153200-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260508-104100-13', 1, '0966882244', 1161000.00, 0.00, 'CASH', '2026-05-08 10:41:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 2, 210000.00, 420000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 3, 12000.00, 36000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-08 10:41:00', 'Bán lẻ tại quầy - Mã đơn HDB-260508-104100-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260511-141800-14', 1, '0912345678', 332000.00, 0.00, 'CASH', '2026-05-11 14:18:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 3, 98000.00, 294000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-11 14:18:00', 'Bán lẻ tại quầy - Mã đơn HDB-260511-141800-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260514-163400-15', 1, '0989261809', 266000.00, 0.00, 'CASH', '2026-05-14 16:34:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 3, 78000.00, 234000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-14 16:34:00', 'Bán lẻ tại quầy - Mã đơn HDB-260514-163400-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260517-141200-16', 1, '0903112233', 396000.00, 0.00, 'CASH', '2026-05-17 14:12:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 2, 38000.00, 76000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 1, 135000.00, 135000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-17 14:12:00', 'Bán lẻ tại quầy - Mã đơn HDB-260517-141200-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260520-201000-17', 2, '0988554433', 990000.00, 0.00, 'CASH', '2026-05-20 20:10:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-05-20 20:10:00', 'Bán lẻ tại quầy - Mã đơn HDB-260520-201000-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260523-081700-18', 1, '0966882244', 1013000.00, 0.00, 'CASH', '2026-05-23 08:17:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 3, 140000.00, 420000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-23 08:17:00', 'Bán lẻ tại quầy - Mã đơn HDB-260523-081700-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260526-111800-19', 1, '0966882244', 258000.00, 0.00, 'TRANSFER', '2026-05-26 11:18:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 2, 12000.00, 24000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 3, 78000.00, 234000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-26 11:18:00', 'Bán lẻ tại quầy - Mã đơn HDB-260526-111800-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260528-145100-20', 1, '0966882244', 1140000.00, 0.00, 'TRANSFER', '2026-05-28 14:51:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 1, 380000.00, 380000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-05-28 14:51:00', 'Bán lẻ tại quầy - Mã đơn HDB-260528-145100-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260602-174700-11', 2, '0989261809', 571000.00, 0.00, 'CASH', '2026-06-02 17:47:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 3, 165000.00, 495000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 2, 38000.00, 76000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-02 17:47:00', 'Bán lẻ tại quầy - Mã đơn HDB-260602-174700-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260605-103000-12', 2, '0966882244', 1229000.00, 0.00, 'CASH', '2026-06-05 10:30:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 2, 165000.00, 330000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 4, 78000.00, 312000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-05 10:30:00', 'Bán lẻ tại quầy - Mã đơn HDB-260605-103000-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260608-153600-13', 2, '0978998877', 624000.00, 0.00, 'TRANSFER', '2026-06-08 15:36:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 3, 98000.00, 294000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 3, 85000.00, 255000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 1, 75000.00, 75000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-08 15:36:00', 'Bán lẻ tại quầy - Mã đơn HDB-260608-153600-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260611-152700-14', 2, '0934556677', 681000.00, 0.00, 'TRANSFER', '2026-06-11 15:27:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 3, 65000.00, 195000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 3, 16000.00, 48000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 4, 85000.00, 340000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-11 15:27:00', 'Bán lẻ tại quầy - Mã đơn HDB-260611-152700-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260614-203500-15', 2, '0912345678', 1694000.00, 0.00, 'CASH', '2026-06-14 20:35:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 4, 16000.00, 64000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 4, 85000.00, 340000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 3, 380000.00, 1140000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-14 20:35:00', 'Bán lẻ tại quầy - Mã đơn HDB-260614-203500-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260617-203100-16', 1, '0988554433', 664000.00, 0.00, 'TRANSFER', '2026-06-17 20:31:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 3, 98000.00, 294000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-06-17 20:31:00', 'Bán lẻ tại quầy - Mã đơn HDB-260617-203100-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260620-154700-17', 2, '0918776655', 970000.00, 0.00, 'TRANSFER', '2026-06-20 15:47:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 3, 210000.00, 630000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 3, 65000.00, 195000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-20 15:47:00', 'Bán lẻ tại quầy - Mã đơn HDB-260620-154700-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260623-082300-18', 2, '0934556677', 937000.00, 0.00, 'CASH', '2026-06-23 08:23:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 1, 135000.00, 135000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 1, 12000.00, 12000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 3, 140000.00, 420000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-23 08:23:00', 'Bán lẻ tại quầy - Mã đơn HDB-260623-082300-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260626-113900-19', 1, '0903112233', 271000.00, 0.00, 'CASH', '2026-06-26 11:39:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 1, 75000.00, 75000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-06-26 11:39:00', 'Bán lẻ tại quầy - Mã đơn HDB-260626-113900-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260628-082700-20', 2, '0912345678', 1144000.00, 0.00, 'TRANSFER', '2026-06-28 08:27:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 3, 65000.00, 195000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 3, 165000.00, 495000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 4, 16000.00, 64000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-06-28 08:27:00', 'Bán lẻ tại quầy - Mã đơn HDB-260628-082700-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260702-144100-11', 2, '0934556677', 1500000.00, 0.00, 'CASH', '2026-07-02 14:41:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 3, 165000.00, 495000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 3, 85000.00, 255000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 4, 145000.00, 580000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 2, 85000.00, 170000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-07-02 14:41:00', 'Bán lẻ tại quầy - Mã đơn HDB-260702-144100-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260705-152700-12', 2, '0934556677', 1332000.00, 0.00, 'TRANSFER', '2026-07-05 15:27:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 3, 210000.00, 630000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 4, 75000.00, 300000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 1, 12000.00, 12000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-07-05 15:27:00', 'Bán lẻ tại quầy - Mã đơn HDB-260705-152700-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260708-173000-13', 2, '0966882244', 1295000.00, 0.00, 'TRANSFER', '2026-07-08 17:30:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 4, 185000.00, 740000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-07-08 17:30:00', 'Bán lẻ tại quầy - Mã đơn HDB-260708-173000-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260711-201600-14', 1, '0912345678', 430000.00, 0.00, 'TRANSFER', '2026-07-11 20:16:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 2, 140000.00, 280000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-11 20:16:00', 'Bán lẻ tại quầy - Mã đơn HDB-260711-201600-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260714-145500-15', 2, '0903112233', 738000.00, 0.00, 'CASH', '2026-07-14 14:55:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 2, 135000.00, 270000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 2, 78000.00, 156000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 2, 140000.00, 280000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-07-14 14:55:00', 'Bán lẻ tại quầy - Mã đơn HDB-260714-145500-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260717-204600-16', 1, '0912345678', 1117000.00, 0.00, 'CASH', '2026-07-17 20:46:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 2, 85000.00, 170000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 4, 98000.00, 392000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-17 20:46:00', 'Bán lẻ tại quầy - Mã đơn HDB-260717-204600-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260720-102100-17', 1, '0903112233', 950000.00, 0.00, 'TRANSFER', '2026-07-20 10:21:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 3, 165000.00, 495000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-20 10:21:00', 'Bán lẻ tại quầy - Mã đơn HDB-260720-102100-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260723-183300-18', 1, '0918776655', 356000.00, 0.00, 'TRANSFER', '2026-07-23 18:33:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 1, 16000.00, 16000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED008'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED008') ORDER BY `id` ASC LIMIT 1), 4, 85000.00, 340000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-23 18:33:00', 'Bán lẻ tại quầy - Mã đơn HDB-260723-183300-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260726-081300-19', 1, '0966882244', 1532000.00, 0.00, 'CASH', '2026-07-26 08:13:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 1, 16000.00, 16000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 4, 145000.00, 580000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 4, 185000.00, 740000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-26 08:13:00', 'Bán lẻ tại quầy - Mã đơn HDB-260726-081300-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260728-112000-20', 1, '0978998877', 1280000.00, 0.00, 'TRANSFER', '2026-07-28 11:20:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 4, 135000.00, 540000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 4, 185000.00, 740000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-07-28 11:20:00', 'Bán lẻ tại quầy - Mã đơn HDB-260728-112000-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260802-173300-11', 1, '0934556677', 767000.00, 0.00, 'TRANSFER', '2026-08-02 17:33:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 2, 85000.00, 170000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 1, 165000.00, 165000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 1, 12000.00, 12000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 3, 140000.00, 420000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-02 17:33:00', 'Bán lẻ tại quầy - Mã đơn HDB-260802-173300-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260805-204900-12', 1, '0989261809', 440000.00, 0.00, 'TRANSFER', '2026-08-05 20:49:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 4, 75000.00, 300000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED022'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED022') ORDER BY `id` ASC LIMIT 1), 1, 140000.00, 140000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-05 20:49:00', 'Bán lẻ tại quầy - Mã đơn HDB-260805-204900-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260808-181900-13', 2, '0934556677', 1465000.00, 0.00, 'CASH', '2026-08-08 18:19:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 4, 145000.00, 580000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 3, 165000.00, 495000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 3, 130000.00, 390000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-08-08 18:19:00', 'Bán lẻ tại quầy - Mã đơn HDB-260808-181900-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260811-171800-14', 1, '0903112233', 905000.00, 0.00, 'CASH', '2026-08-11 17:18:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 4, 135000.00, 540000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 1, 365000.00, 365000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-11 17:18:00', 'Bán lẻ tại quầy - Mã đơn HDB-260811-171800-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260814-204500-15', 2, '0934556677', 1325000.00, 0.00, 'CASH', '2026-08-14 20:45:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 1, 365000.00, 365000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 3, 135000.00, 405000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 3, 185000.00, 555000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-08-14 20:45:00', 'Bán lẻ tại quầy - Mã đơn HDB-260814-204500-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260817-081500-16', 1, '0989261809', 1990000.00, 0.00, 'CASH', '2026-08-17 08:15:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 4, 98000.00, 392000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 4, 380000.00, 1520000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 1, 78000.00, 78000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-17 08:15:00', 'Bán lẻ tại quầy - Mã đơn HDB-260817-081500-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260820-103000-17', 1, '0912345678', 406000.00, 0.00, 'CASH', '2026-08-20 10:30:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 3, 12000.00, 36000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-20 10:30:00', 'Bán lẻ tại quầy - Mã đơn HDB-260820-103000-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260823-094800-18', 1, '0966882244', 854000.00, 0.00, 'TRANSFER', '2026-08-23 09:48:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 4, 165000.00, 660000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 2, 65000.00, 130000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 4, 16000.00, 64000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-08-23 09:48:00', 'Bán lẻ tại quầy - Mã đơn HDB-260823-094800-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260826-114200-19', 2, '0988554433', 1775000.00, 0.00, 'CASH', '2026-08-26 11:42:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 2, 65000.00, 130000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 4, 365000.00, 1460000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-08-26 11:42:00', 'Bán lẻ tại quầy - Mã đơn HDB-260826-114200-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260828-193400-20', 2, '0912345678', 732000.00, 0.00, 'TRANSFER', '2026-08-28 19:34:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED045'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED045') ORDER BY `id` ASC LIMIT 1), 4, 78000.00, 312000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 2, 210000.00, 420000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-08-28 19:34:00', 'Bán lẻ tại quầy - Mã đơn HDB-260828-193400-20');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260902-142500-11', 2, '0934556677', 1706000.00, 0.00, 'TRANSFER', '2026-09-02 14:25:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 5, 145000.00, 725000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 1, 135000.00, 135000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 5, 130000.00, 650000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-09-02 14:25:00', 'Bán lẻ tại quầy - Mã đơn HDB-260902-142500-11');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260905-152300-12', 1, '0903112233', 1755000.00, 0.00, 'TRANSFER', '2026-09-05 15:23:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 4, 380000.00, 1520000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED146'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED146') ORDER BY `id` ASC LIMIT 1), 1, 85000.00, 85000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED054'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED054') ORDER BY `id` ASC LIMIT 1), 2, 75000.00, 150000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-09-05 15:23:00', 'Bán lẻ tại quầy - Mã đơn HDB-260905-152300-12');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260908-111300-13', 2, '0903112233', 478000.00, 0.00, 'CASH', '2026-09-08 11:13:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 2, 135000.00, 270000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED003'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED003') ORDER BY `id` ASC LIMIT 1), 1, 12000.00, 12000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED026'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED026') ORDER BY `id` ASC LIMIT 1), 2, 98000.00, 196000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-09-08 11:13:00', 'Bán lẻ tại quầy - Mã đơn HDB-260908-111300-13');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260911-205500-14', 1, '0918776655', 2070000.00, 0.00, 'CASH', '2026-09-11 20:55:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 1, 210000.00, 210000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 2, 135000.00, 270000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED061'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED061') ORDER BY `id` ASC LIMIT 1), 4, 365000.00, 1460000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 1, 130000.00, 130000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-09-11 20:55:00', 'Bán lẻ tại quầy - Mã đơn HDB-260911-205500-14');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260914-141500-15', 1, '0989261809', 763000.00, 0.00, 'CASH', '2026-09-14 14:15:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 5, 145000.00, 725000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED027'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED027') ORDER BY `id` ASC LIMIT 1), 1, 38000.00, 38000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-09-14 14:15:00', 'Bán lẻ tại quầy - Mã đơn HDB-260914-141500-15');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260917-172600-16', 1, '0903112233', 1738000.00, 0.00, 'CASH', '2026-09-17 17:26:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED071'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED071') ORDER BY `id` ASC LIMIT 1), 4, 130000.00, 520000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED145'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED145') ORDER BY `id` ASC LIMIT 1), 1, 98000.00, 98000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 5, 185000.00, 925000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED024'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED024') ORDER BY `id` ASC LIMIT 1), 3, 65000.00, 195000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-09-17 17:26:00', 'Bán lẻ tại quầy - Mã đơn HDB-260917-172600-16');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260920-114400-17', 2, '0918776655', 1420000.00, 0.00, 'CASH', '2026-09-20 11:44:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED002'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED002') ORDER BY `id` ASC LIMIT 1), 4, 210000.00, 840000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 4, 145000.00, 580000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-09-20 11:44:00', 'Bán lẻ tại quầy - Mã đơn HDB-260920-114400-17');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260923-181500-18', 2, '0903112233', 1295000.00, 0.00, 'TRANSFER', '2026-09-23 18:15:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 2, 185000.00, 370000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED073'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED073') ORDER BY `id` ASC LIMIT 1), 5, 185000.00, 925000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-09-23 18:15:00', 'Bán lẻ tại quầy - Mã đơn HDB-260923-181500-18');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260926-153500-19', 2, '0903112233', 415000.00, 0.00, 'TRANSFER', '2026-09-26 15:35:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED001'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED001') ORDER BY `id` ASC LIMIT 1), 1, 145000.00, 145000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED043'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED043') ORDER BY `id` ASC LIMIT 1), 2, 135000.00, 270000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 2, '2026-09-26 15:35:00', 'Bán lẻ tại quầy - Mã đơn HDB-260926-153500-19');

INSERT INTO `sales_orders` (`code`, `employee_id`, `customer_phone`, `total_amount`, `discount`, `payment_method`, `created_at`) VALUES
('HDB-260928-194300-20', 1, '0918776655', 1257000.00, 0.00, 'TRANSFER', '2026-09-28 19:43:00');
SET @order_id = LAST_INSERT_ID();
INSERT INTO `sales_order_items` (`sales_order_id`, `medicine_id`, `inventory_lot_id`, `quantity`, `unit_price`, `line_total`) VALUES
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED041'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED041') ORDER BY `id` ASC LIMIT 1), 2, 16000.00, 32000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED021'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED021') ORDER BY `id` ASC LIMIT 1), 1, 185000.00, 185000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED066'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED066') ORDER BY `id` ASC LIMIT 1), 4, 165000.00, 660000.00),
(@order_id, (SELECT `id` FROM `medicines` WHERE `code` = 'MED004'), (SELECT `id` FROM `inventory_lots` WHERE `medicine_id` = (SELECT `id` FROM `medicines` WHERE `code` = 'MED004') ORDER BY `id` ASC LIMIT 1), 1, 380000.00, 380000.00);
INSERT INTO `inventory_transactions` (`type`, `sales_order_id`, `employee_id`, `transaction_at`, `note`) VALUES
('EXPORT_SALE', @order_id, 1, '2026-09-28 19:43:00', 'Bán lẻ tại quầy - Mã đơn HDB-260928-194300-20');

SET FOREIGN_KEY_CHECKS = 1;
