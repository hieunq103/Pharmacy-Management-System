SET SESSION cte_max_recursion_depth = 1000000;
SET FOREIGN_KEY_CHECKS=0;
-- 5000 thuoc
INSERT INTO medicines (code,name,active_ingredient,unit,category_id,manufacturer,sell_price,requires_prescription,min_stock_threshold,status)
WITH RECURSIVE s(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM s WHERE n<5000)
SELECT CONCAT('BEN',LPAD(n,6,'0')), CONCAT('Thuoc bench ',n), CONCAT('Hoat chat ',n%300), 'Hop', 1+(n%11), 'NSX', 10000+(n%50)*1000, n%5=0, 20, 'ACTIVE' FROM s;
-- 2000 giao dich nhap, moi giao dich 25 lo => 50k lo
INSERT INTO inventory_transactions (type,supplier_id,employee_id,transaction_at,note)
WITH RECURSIVE s(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM s WHERE n<2000)
SELECT 'IMPORT', 1+(n%6), 1, DATE_SUB('2026-10-25 08:00:00', INTERVAL n*4 HOUR), CONCAT('Bench import ',n) FROM s;
INSERT INTO inventory_lots (medicine_id,transaction_id,lot_number,expiry_date,import_price,quantity_in,quantity_remaining)
WITH RECURSIVE s(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM s WHERE n<49999)
SELECT (SELECT MIN(id) FROM medicines WHERE code LIKE 'BEN%') + (n % 5000),
       (SELECT MIN(id) FROM inventory_transactions WHERE note LIKE 'Bench import%') + (n DIV 25),
       CONCAT('BL',n), DATE_ADD('2026-08-01', INTERVAL (n%900) DAY), 8000, 100, IF(n%7=0,0,(n%100)+1) FROM s;
-- 100k don ban, 3 dong/don
INSERT INTO sales_orders (code,employee_id,customer_phone,total_amount,discount,payment_method,created_at)
WITH RECURSIVE s(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM s WHERE n<100000)
SELECT CONCAT('BENCH-',n), 1+(n%2), NULL, 30000, 0, IF(n%2=0,'CASH','TRANSFER'), DATE_SUB('2026-10-26 20:00:00', INTERVAL n*7 MINUTE) FROM s;
INSERT INTO sales_order_items (sales_order_id,medicine_id,inventory_lot_id,quantity,unit_price,line_total)
WITH RECURSIVE s(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM s WHERE n<299999)
SELECT (SELECT MIN(id) FROM sales_orders WHERE code LIKE 'BENCH-%') + (n DIV 3),
       (SELECT MIN(id) FROM medicines WHERE code LIKE 'BEN%') + ((n*7) % 5000),
       (SELECT MIN(id) FROM inventory_lots WHERE lot_number LIKE 'BL%') + ((n*7) % 5000) + 5000*(n%3),
       1, 10000, 10000 FROM s;
INSERT INTO inventory_transactions (type,sales_order_id,employee_id,transaction_at,note)
SELECT 'EXPORT_SALE', id, employee_id, created_at, CONCAT('Ban le ',code) FROM sales_orders WHERE code LIKE 'BENCH-%';
SET FOREIGN_KEY_CHECKS=1;
ANALYZE TABLE medicines, inventory_lots, sales_orders, sales_order_items, inventory_transactions;
