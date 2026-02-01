# E-Commerce (hsf302-mvc)

Ứng dụng thương mại điện tử xây dựng bằng **Spring Boot**, **Thymeleaf** (SSR), **PostgreSQL**, **MinIO**. Hỗ trợ đăng ký/đăng nhập, giỏ hàng, đơn hàng, thanh toán COD/VNPay, vận chuyển GHN, đánh giá, KYC, quản trị.

---

## Yêu cầu

- **Java 21**
- **Maven 3.6+**
- **Docker** và **Docker Compose** (để chạy PostgreSQL + MinIO)
- Git

---

## 1. Clone và vào thư mục dự án

```bash
git clone <URL_REPO_CỦA_BẠN> hsf302-mvc
cd hsf302-mvc
```

Thay `<URL_REPO_CỦA_BẠN>` bằng URL thực tế (ví dụ `https://github.com/username/hsf302-mvc.git`).

---

## 2. Cấu hình biến môi trường (.env)

Tạo file `.env` ở **thư mục gốc** của dự án (cùng cấp với `docker-compose.yml`), copy nội dung từ `env.example` rồi điền giá trị.

```bash
cp env.example .env
```

Chỉnh sửa `.env` với các biến **bắt buộc**:

| Biến | Mô tả | Ví dụ (dev) |
|------|--------|--------------|
| `POSTGRES_DB` | Tên database PostgreSQL | `ecommerce` |
| `POSTGRES_USER` | User đăng nhập DB | `postgres` |
| `POSTGRES_PASSWORD` | Mật khẩu DB | `postgres` |
| `POSTGRES_PORT` | Port PostgreSQL trên máy host (app kết nối qua port này) | `5432` |
| `MINIO_ROOT_USER` | User MinIO | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | Mật khẩu MinIO | `minioadmin` |
| `MINIO_API_PORT` | Port API MinIO trên host | `9000` |
| `MINIO_CONSOLE_PORT` | Port Console MinIO | `9001` |
| `MINIO_BUCKET_NAME` | Tên bucket lưu file (upload ảnh, v.v.) | `ecommerce` |
| `JWT_SECRET` | Secret ký JWT (chuỗi bí mật, đủ dài) | Chuỗi bất kỳ ≥ 32 ký tự |
| `JWT_EXPIRATION` | Thời hạn access token (ms) | `86400000` |
| `JWT_REFRESH_EXPIRATION` | Thời hạn refresh token (ms) | `604800000` |

**Lưu ý:** Ứng dụng đọc **`POSTGRES_PORT`** trong `application.properties` (mặc định `5432`). Nếu trong `env.example` có `DB_PORT`, bạn nên đặt thêm `POSTGRES_PORT` cùng giá trị (ví dụ `POSTGRES_PORT=5432`) để trùng với cấu hình trong `docker-compose.yml`.

Các biến **tùy chọn** (có thể để trống khi chỉ chạy dev cơ bản):

- **Mail:** `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` — dùng cho xác thực email; nếu trống, chức năng gửi mail có thể lỗi khi gọi.
- **APP_BASE_URL** — URL gốc ứng dụng (mặc định `http://localhost:8080`), dùng trong link email.
- **GHN:** `GHN_URL`, `GHN_TOKEN`, `GHN_SHOP_ID` — tích hợp Giao Hàng Nhanh (tính phí, tạo vận đơn).
- **VNPay:** `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_URL`, `VNPAY_RETURN_URL` — thanh toán VNPay.
- **VNPT eKYC:** `VNPT_EKYC_*` — xác thực KYC.

---

## 3. Chạy Docker (PostgreSQL + MinIO)

Đảm bảo Docker đang chạy. Từ thư mục gốc dự án:

```bash
docker compose up -d
```

Lệnh này sẽ:

1. Tạo network `backend`.
2. Chạy **PostgreSQL** và **MinIO** (và container khởi tạo MinIO).

### Giải thích các service trong Docker

| Service | Image | Mục đích |
|--------|--------|----------|
| **postgres** | `postgres:17-alpine` | Database chính của ứng dụng. Lưu user, đơn hàng, sản phẩm, thanh toán, v.v. Port map `${POSTGRES_PORT}:5432` — ứng dụng kết nối qua `localhost:POSTGRES_PORT`. |
| **minio** | `minio/minio:latest` | Object storage tương thích S3. Dùng lưu file upload (ảnh sản phẩm, ảnh review, KYC, v.v.). Port 9000 = API, 9001 = Web Console. |
| **minio-init** | `minio/mc:latest` | Container chạy **một lần** sau khi MinIO đã sẵn sàng: tạo bucket (tên trong `MINIO_BUCKET_NAME`), set policy public read cho bucket đó. Script chạy nằm trong file `minio-init.sh` trên máy host. |

**Volumes:** `postgres_data` và `minio_data` giữ dữ liệu khi bạn `docker compose down`. Chỉ khi xóa volume thì mới mất dữ liệu.

**Kiểm tra:** Sau khi `up -d`, có thể vào MinIO Console: `http://localhost:9001` (đăng nhập bằng `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`). PostgreSQL có thể dùng client (DBeaver, pgAdmin) kết nối `localhost:5432` (hoặc đúng `POSTGRES_PORT`).

---

## 4. File `minio-init.sh` làm gì?

File `minio-init.sh` được mount vào container **minio-init** và chạy bằng `mc` (MinIO Client):

1. **Chờ MinIO sẵn sàng** — lặp set alias `myminio` tới `http://minio:9000` cho tới khi thành công (timeout 60 lần × 2s).
2. **Tạo bucket** — tên lấy từ `MINIO_BUCKET_NAME` (mặc định `ecommerce`). Nếu đã tồn tại thì bỏ qua.
3. **Set policy** — `mc anonymous set download` cho bucket đó để cho phép đọc file (public read), phục vụ xem ảnh qua URL.

Nhờ vậy lần đầu `docker compose up` xong, bucket đã sẵn sàng; ứng dụng chỉ cần cấu hình đúng `minio.bucket-name`, `minio.url`, `minio.access-key`, `minio.secret-key` (lấy từ env) là upload/đọc file được.

---

## 5. Cấu hình Hibernate `ddl-auto=update` trong application.properties

Trong `src/main/resources/application.properties` có:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Ý nghĩa:**

- **update** = Hibernate so sánh entity (Order, User, Product, Payment, …) với schema hiện tại trong PostgreSQL. Nếu thiếu bảng/cột thì **tự tạo hoặc thêm cột**; **không xóa** cột hay bảng đã có.
- Thuận tiện cho **development**: không cần chạy script SQL tạo bảng thủ công; chỉ cần sửa entity và restart app là schema cập nhật.
- **Rủi ro production:** thay đổi entity có thể sinh ra thay đổi schema không kiểm soát, hoặc migration phức tạp. Môi trường production nên dùng `validate` (chỉ kiểm tra schema khớp entity, không sửa DB) và quản lý schema bằng Flyway/Liquibase.

Tóm lại: với mục đích clone về và chạy nhanh, `update` giúp bạn không phải tạo bảng tay; chỉ cần Docker (PostgreSQL) + `.env` đúng là có thể chạy app.

---

## 6. DataInitializer (package `config`) — tạo dữ liệu mặc định khi chạy app

Trong package `com.hsf.e_comerce.config` có class **`DataInitializer`** implements `CommandLineRunner`. Mỗi lần **ứng dụng khởi động**, Spring sẽ gọi `run(...)` **một lần**. Class này:

1. **Roles** — Tạo 3 role nếu chưa có: `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`.
2. **Platform settings** — Tạo cấu hình hoa hồng nền tảng: key `commission_rate` = `10` (10%) nếu chưa có.
3. **Default users** — Tạo 3 tài khoản mặc định (nếu chưa tồn tại theo email):
   - **Buyer:** `buyer@gmail.com` / `buyer123@` — role BUYER, đã coi là xác minh email.
   - **Seller:** `seller@gmail.com` / `seller123@` — role SELLER, đã xác minh email; đồng thời tạo **1 shop mặc định** cho user này.
   - **Admin:** `admin@gmail.com` / `admin123@` — role ADMIN, đã xác minh email.
4. **Product categories** — Tạo cây danh mục: 1 root “Điện Tử” và nhiều danh mục con (Điện Thoại, Laptop, Tai Nghe, …).
5. **Sample shops / products / orders** — Chỉ chạy khi **chưa có đơn hàng nào** (`orderRepository.count() == 0`):
   - Cập nhật tên/mô tả shop của seller thành “TechZone – Đồ Điện Tử”.
   - Tạo **20 sản phẩm** mẫu (điện thoại, laptop, tai nghe, loa, sạc, USB, màn hình, bàn phím, chuột, smartwatch, router, webcam, ổ cứng, thẻ nhớ, tivi, máy ảnh, …) với ảnh từ Unsplash.
   - Tạo **7 đơn hàng mẫu** với trạng thái khác nhau: DELIVERED, CONFIRMED, PROCESSING, SHIPPING, PENDING_PAYMENT, CANCELLED.

Logic “chỉ tạo khi chưa có” (role, user, category, hoặc order) tránh trùng dữ liệu khi restart app nhiều lần.

---

## 7. Tài khoản có sẵn (sau lần chạy app đầu tiên)

Sau khi Docker đã chạy, `.env` đã cấu hình đúng và bạn chạy ứng dụng **lần đầu**, DataInitializer sẽ tạo các tài khoản sau. Dùng để đăng nhập và test:

| Vai trò | Email | Mật khẩu | Ghi chú |
|---------|--------|----------|--------|
| **Người mua (Buyer)** | `buyer@gmail.com` | `buyer123@` | Đặt hàng, xem đơn, đánh giá, dùng giỏ hàng. |
| **Người bán (Seller)** | `seller@gmail.com` | `seller123@` | Có 1 shop (TechZone), 20 sản phẩm mẫu và đơn mẫu; quản lý đơn, sản phẩm, shop. |
| **Quản trị (Admin)** | `admin@gmail.com` | `admin123@` | Quản lý user, đơn, sản phẩm, cấu hình hoa hồng, duyệt seller, xử lý báo cáo review, v.v. |

Tất cả đều được set **email đã xác minh** (`emailVerified = true`) để không bị chặn bởi luồng xác thực email khi test.

---

## 8. Chạy ứng dụng

Sau khi đã:

1. Clone và `cd` vào dự án  
2. Tạo `.env` từ `env.example` và điền ít nhất PostgreSQL, MinIO, JWT  
3. Chạy `docker compose up -d`  
4. Đảm bảo port PostgreSQL và MinIO không bị trùng với service khác  

Chạy Spring Boot:

```bash
./mvnw spring-boot:run
```

Windows (CMD/PowerShell):

```cmd
mvnw.cmd spring-boot:run
```

Ứng dụng mặc định chạy tại **http://localhost:8080**.

- Trang chủ: http://localhost:8080  
- Đăng nhập: http://localhost:8080/login  
- Sau khi đăng nhập có thể vào **Profile**, **Giỏ hàng**, **Đơn hàng**, **Sản phẩm** (buyer); hoặc **Seller** (đơn, sản phẩm, shop); hoặc **Admin** (dashboard, users, orders, commission, …).

---

## 9. Tóm tắt nhanh

```bash
git clone <REPO_URL> hsf302-mvc
cd hsf302-mvc
cp env.example .env
# Chỉnh .env: POSTGRES_*, MINIO_*, JWT_* (bắt buộc)
docker compose up -d
./mvnw spring-boot:run
```

Mở http://localhost:8080, đăng nhập bằng `buyer@gmail.com` / `buyer123@` (hoặc seller/admin như bảng trên).

---

## Tài liệu thêm

- **docs/Payment.readme** — Đặc tả Payment (COD, VNPay, refund, trạng thái).
- **docs/VIEC_CAN_LAM.md** — Việc cần làm (Refund, Multi-payment, Wallet, Payout) và phân công.

Nếu bạn gặp lỗi khi clone và chạy (DB connection, MinIO, port), kiểm tra lại `.env` và `docker compose ps` để đảm bảo postgres và minio đang chạy đúng port.
