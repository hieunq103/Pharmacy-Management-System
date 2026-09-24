# pharmacy-desktop

Khung dự án Maven cho ứng dụng quản lý cửa hàng thuốc (JavaFX + MySQL). Cấu trúc bám sát **Module Boundary** và **Ownership & Dependency Map** trong tài liệu thiết kế hệ thống.

## Cây thư mục đầy đủ

```
pharmacy-desktop/
├── pom.xml                          ← Khai báo dependency & plugin build (Maven)
├── README.md                        ← File này
├── .gitignore
│
├── src/main/java/com/pharmacy/
│   ├── PharmacyApplication.java     ← Entry point JavaFX, KHÔNG chứa business logic
│   │
│   ├── auth/                        ← Đăng nhập/đăng xuất, session, phân quyền (FR-01–03)
│   ├── employee/                    ← Hồ sơ nhân viên (FR-10–12)
│   ├── account/                     ← Tài khoản đăng nhập gắn nhân viên (FR-20–23)
│   ├── catalog/                     ← Danh mục thuốc, nhóm thuốc (FR-30)
│   ├── supplier/                    ← Nhà cung cấp (FR-35)
│   ├── inventory/                   ← Nhập/xuất kho, tồn kho, FEFO (FR-31–34)
│   ├── sales/                       ← Bán hàng, giỏ hàng, hóa đơn (FR-40–45)
│   ├── report/                      ← Báo cáo doanh số/số lượng bán (FR-50–53)
│   │
│   ├── common/                      ← Dùng chung, KHÔNG phụ thuộc module nghiệp vụ nào
│   │   ├── exception/                  BusinessException, SystemException và các lớp con
│   │   ├── validation/                 Validator dùng chung (CCCD, SĐT, email...)
│   │   ├── util/                       DateUtil, MoneyFormatter, PasswordHasher...
│   │   └── dto/                        PageResponse<T>, ErrorResponse — theo Error/Pagination
│   │                                   contract (mục 8 tài liệu thiết kế)
│   │
│   └── infra/                       ← Hạ tầng kỹ thuật, KHÔNG chứa nghiệp vụ
│       ├── config/                     Đọc application.properties, khởi tạo AppConfig
│       ├── db/                         HikariCP DataSource, Flyway MigrationRunner,
│       │                               EntityManagerFactory (Hibernate)
│       └── security/                   BCrypt wrapper, session/token quản lý đăng nhập
│
├── src/main/resources/
│   ├── fxml/                        ← Giao diện .fxml, 1 thư mục con / 1 module UI
│   │   ├── auth/                       login.fxml
│   │   ├── dashboard/                  main-shell.fxml (sidebar + topbar dùng chung)
│   │   ├── employee/
│   │   ├── account/
│   │   ├── catalog/
│   │   ├── inventory/
│   │   ├── sales/                      pos.fxml (màn hình bán hàng)
│   │   └── report/
│   ├── css/
│   │   └── app.css                  ← Design token màu/font, đồng bộ với bản demo UI
│   ├── db/migration/                ← Flyway: V1__..., V2__... (không sửa file đã merge)
│   ├── application.properties       ← Cấu hình DB, timeout, auth
│   └── logback.xml                  ← Cấu hình log xoay vòng (giữ 30 ngày)
│
└── src/test/java/com/pharmacy/
    ├── auth/ employee/ account/ catalog/ supplier/ inventory/ sales/ report/
    │                                 ← Unit test Service layer (mục 13.7, coverage ≥ 80%)
    └── (integration test dùng Testcontainers sẽ thêm ở src/test/java/.../it/ khi cần)
```

## Nhiệm vụ từng nhóm thư mục

### 1. `com.pharmacy.<module>` — 8 module nghiệp vụ
Mỗi module (`auth`, `employee`, `account`, `catalog`, `supplier`, `inventory`, `sales`, `report`) đều có đúng 5 thư mục con, tương ứng 1 tầng trong kiến trúc phân lớp:

| Thư mục con | Nhiệm vụ | Được phép làm | Không được làm |
|---|---|---|---|
| `controller/` | Điều phối View ↔ Service, bắt sự kiện JavaFX (`onAction`, `initialize`) | Validate input định dạng, điều hướng màn hình, disable/enable nút | Chứa business rule, gọi Repository trực tiếp, gọi DB/mạng đồng bộ trên UI thread |
| `service/` | Toàn bộ business logic + transaction boundary | Gọi Repository của module khác qua Service interface công khai của module đó | Biết gì về JavaFX (không import `javafx.*`) |
| `repository/` | Truy vấn dữ liệu (CRUD, query) qua Hibernate/JPA | Viết query, mapping Entity ↔ bảng | Chứa business rule (vd. tính FEFO phải nằm ở `inventory/service`, không ở `repository`) |
| `model/` | JPA Entity ánh xạ 1-1 với bảng trong Data Dictionary | | Đưa thẳng ra `controller`/UI — luôn đi qua `dto` |
| `dto/` | Data Transfer Object dùng để giao tiếp giữa `controller` và `service`, và để trả dữ liệu ra UI | | Chứa logic nghiệp vụ |

### 2. `common/` — dùng chung toàn hệ thống
Chứa exception, validator, formatter, DTO chuẩn (`PageResponse`, `ErrorResponse`) dùng lại ở mọi module. Theo Dependency Map: **mọi module đều được phép phụ thuộc vào `common`, nhưng `common` không được phụ thuộc ngược lại bất kỳ module nghiệp vụ nào.**

### 3. `infra/` — hạ tầng kỹ thuật
Chứa cấu hình kết nối DB (HikariCP), chạy Flyway migration khi khởi động, và các tiện ích bảo mật (hash mật khẩu, quản lý session). Cùng nguyên tắc một chiều như `common`.

### 4. `resources/fxml/`
Tổ chức 1-1 với module UI — dễ tìm file `.fxml` tương ứng khi biết đang sửa màn hình nào. `dashboard/` là ngoại lệ: chứa khung sườn dùng chung (sidebar, topbar) mà các màn hình khác nhúng vào, giống cấu trúc trong bản demo UI đã làm.

### 5. `resources/db/migration/`
Nơi duy nhất được phép thay đổi schema DB, theo quy tắc **Migration Governance** (mục 12 tài liệu thiết kế): đặt tên `V<version>__<mô_tả>.sql`, không sửa lại file đã merge, mỗi file có kèm rollback thủ công ghi trong PR.

### 6. `src/test/java/...`
Mirror cấu trúc `src/main` theo module — test ở đâu thì đặt cùng package với code được test (chuẩn Maven). Ưu tiên viết test cho `service/` trước (nơi chứa business rule quan trọng nhất), đặc biệt `sales` và `inventory` (chống bán âm kho — mục 13.4).

## Bước tiếp theo (khớp Tuần 1 trong kế hoạch 4 tuần)

1. Mở `pom.xml` bằng IDE (IntelliJ/VS Code), để Maven tải dependency lần đầu.
2. Cấu hình lại `src/main/resources/application.properties` với thông tin MySQL thật trên máy bạn.
3. Chạy thử Flyway: `V1__init_schema.sql` hiện mới có bảng `roles` — điền tiếp `employees`, `accounts`, `login_history` theo Data Dictionary trước khi code `auth/service`.
4. Viết `PharmacyApplication.java` → nạp `AppConfig` → chạy `MigrationRunner.migrate()` → mở `fxml/auth/login.fxml`.
