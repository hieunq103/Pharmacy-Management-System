# Benchmark truy vấn

Đo **số câu SQL** (chênh lệch `Com_select + Com_insert + Com_update + Com_delete` của MySQL), **số dòng InnoDB đọc**
(`Innodb_rows_read`) và **thời gian** trung bình cho từng phương thức repository dùng ở Dashboard / Báo cáo / Kho / Bán hàng.

```bash
# 1. Tạo DB, chạy migration V1..V4 (hoặc V1..V5) rồi sinh dữ liệu lớn
mysql -uroot -e "CREATE DATABASE pharmacy_bench CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
export DB_URL='jdbc:mysql://localhost:3306/pharmacy_bench?serverTimezone=Asia/Ho_Chi_Minh' DB_USER=... DB_PASSWORD=...
mvn compile exec:java -Dexec.mainClass=com.pharmacy.infra.db.MigrationRunner
mysql -uroot pharmacy_bench < docs/benchmark/generate_bench_data.sql

# 2. Biên dịch và chạy (đọc) / (ghi)
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
CP=target/classes:$(cat cp.txt)
javac -d /tmp/bench -cp $CP docs/benchmark/QueryBench.java
java -cp /tmp/bench:$CP QueryBench          # truy vấn đọc
java -cp /tmp/bench:$CP QueryBench writes   # thanh toán / nhập / xuất kho
```

Chạy trên một DB không có ai khác truy cập (bộ đếm là GLOBAL STATUS).
