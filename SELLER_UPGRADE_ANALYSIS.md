# Phân Tích: Upgrade User Lên Seller

**Ngày:** 11-01-2026  
**Phiên bản:** 1.0

---

## 1. Tổng Quan

### 1.1. Mục Đích
Phân tích và đề xuất giải pháp cho tính năng cho phép người dùng (BUYER) nâng cấp tài khoản lên SELLER để có thể bán hàng trên sàn thương mại điện tử.

### 1.2. Bối Cảnh
- Hiện tại: User đăng ký mặc định có role `ROLE_BUYER`
- Mục tiêu: User có thể yêu cầu upgrade lên `ROLE_SELLER` để mở shop và bán hàng
- Theo SRS: `ROLE_SELLER` có tất cả quyền của `ROLE_BUYER` + quyền quản lý shop

---

## 2. Phân Tích Yêu Cầu

### 2.1. Quy Trình Upgrade

#### **Option 1: Tự Động (Không cần duyệt)**
- User điền form đăng ký seller
- Hệ thống tự động gán role `ROLE_SELLER`
- User có thể tạo shop ngay lập tức

**Ưu điểm:**
- Đơn giản, nhanh chóng
- Trải nghiệm người dùng tốt

**Nhược điểm:**
- Không kiểm soát chất lượng seller
- Có thể có seller không đáng tin cậy
- Khó quản lý gian hàng spam

#### **Option 2: Cần Admin Duyệt (Đề xuất)**
- User điền form đăng ký seller
- Hệ thống tạo request với status `PENDING`
- Admin xem và duyệt/từ chối request
- Nếu duyệt: Gán role `ROLE_SELLER` + Tạo shop với status `ACTIVE`
- Nếu từ chối: Giữ nguyên role `ROLE_BUYER`, gửi thông báo lý do

**Ưu điểm:**
- Kiểm soát chất lượng seller
- Tránh spam và gian hàng không hợp lệ
- Có thể yêu cầu thông tin xác thực (CMND, giấy phép kinh doanh)

**Nhược điểm:**
- Cần thời gian chờ duyệt
- Cần có admin để xử lý

### 2.2. Thông Tin Cần Thu Thập

#### **Thông Tin Bắt Buộc:**
1. **Thông tin Shop:**
   - Tên shop (unique)
   - Mô tả shop
   - Số điện thoại shop
   - Địa chỉ shop
   - Logo shop (upload)
   - Ảnh bìa shop (upload, optional)

2. **Thông Tin Xác Thực (nếu cần):**
   - CMND/CCCD (upload ảnh)
   - Giấy phép kinh doanh (nếu có)
   - Email xác nhận
   - Số điện thoại xác nhận

#### **Thông Tin Tùy Chọn:**
- Website shop
- Mạng xã hội (Facebook, Instagram)
- Giờ làm việc
- Chính sách đổi trả

### 2.3. Business Rules

1. **Một User chỉ có một Shop:**
   - Kiểm tra user đã có shop chưa
   - Nếu đã có shop → Không cho tạo mới

2. **Tên Shop phải unique:**
   - Kiểm tra tên shop đã tồn tại chưa
   - Gợi ý tên khác nếu trùng

3. **User phải đã xác thực:**
   - Email đã verify (nếu có tính năng này)
   - Số điện thoại đã xác nhận
   - Tài khoản đang active

4. **Validation:**
   - Tên shop: 3-100 ký tự
   - Mô tả: Tối đa 2000 ký tự
   - Số điện thoại: Format hợp lệ
   - Địa chỉ: Không được để trống

---

## 3. Thiết Kế Database

### 3.1. Bảng `seller_requests` (Mới)

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | UUID | PK |
| user_id | UUID | FK → users.id |
| shop_name | VARCHAR(255) | Tên shop yêu cầu |
| shop_description | TEXT | Mô tả shop |
| shop_phone | VARCHAR(20) | Số điện thoại shop |
| shop_address | VARCHAR(255) | Địa chỉ shop |
| logo_url | VARCHAR(255) | URL logo |
| cover_image_url | VARCHAR(255) | URL ảnh bìa |
| status | VARCHAR(50) | PENDING, APPROVED, REJECTED |
| rejection_reason | TEXT | Lý do từ chối (nếu có) |
| reviewed_by | UUID | FK → users.id (admin) |
| reviewed_at | TIMESTAMP | Thời gian duyệt |
| created_at | TIMESTAMP | Thời gian tạo request |
| updated_at | TIMESTAMP | Thời gian cập nhật |

### 3.2. Cập Nhật Bảng `shops`

- Thêm constraint: `user_id` phải unique (một user một shop)
- Status: `PENDING`, `ACTIVE`, `INACTIVE`, `BANNED`
- Khi tạo shop từ seller request → status = `ACTIVE`

---

## 4. Workflow Đề Xuất

### 4.1. User Request Upgrade

```
1. User đăng nhập (phải có role BUYER)
2. Vào trang "Trở thành Seller" hoặc "Mở Shop"
3. Điền form:
   - Tên shop
   - Mô tả shop
   - Số điện thoại
   - Địa chỉ
   - Upload logo
   - Upload ảnh bìa (optional)
4. Submit request
5. Hệ thống tạo seller_request với status = PENDING
6. Gửi thông báo cho admin
7. Hiển thị thông báo cho user: "Yêu cầu đã được gửi, đang chờ duyệt"
```

### 4.2. Admin Duyệt Request

```
1. Admin đăng nhập
2. Vào trang "Quản lý yêu cầu Seller"
3. Xem danh sách requests PENDING
4. Xem chi tiết request:
   - Thông tin user
   - Thông tin shop yêu cầu
   - Logo, ảnh bìa
5. Quyết định:
   
   a) APPROVE:
      - Cập nhật seller_request.status = APPROVED
      - Gán role ROLE_SELLER cho user
      - Tạo shop với thông tin từ request
      - Set shop.status = ACTIVE
      - Gửi email thông báo cho user
   
   b) REJECT:
      - Cập nhật seller_request.status = REJECTED
      - Điền rejection_reason
      - Gửi email thông báo lý do từ chối
      - User có thể chỉnh sửa và gửi lại
```

### 4.3. User Sau Khi Được Duyệt

```
1. Nhận thông báo/email: "Yêu cầu đã được duyệt"
2. Đăng nhập lại → Thấy menu "Quản lý Shop"
3. Có thể:
   - Xem thông tin shop
   - Chỉnh sửa thông tin shop
   - Thêm sản phẩm
   - Quản lý đơn hàng
```

---

## 5. API Endpoints Đề Xuất

### 5.1. User Endpoints

```
POST   /api/seller/request          - Tạo request upgrade
GET    /api/seller/request/status   - Xem trạng thái request
PUT    /api/seller/request          - Cập nhật request (nếu bị reject)
DELETE /api/seller/request          - Hủy request (nếu chưa duyệt)
```

### 5.2. Admin Endpoints

```
GET    /api/admin/seller-requests           - Danh sách requests
GET    /api/admin/seller-requests/{id}      - Chi tiết request
POST   /api/admin/seller-requests/{id}/approve  - Duyệt request
POST   /api/admin/seller-requests/{id}/reject   - Từ chối request
```

---

## 6. Frontend Pages

### 6.1. User Pages

1. **Trang "Trở thành Seller"** (`/become-seller`)
   - Form đăng ký seller
   - Upload logo, ảnh bìa
   - Preview shop
   - Hiển thị trạng thái request (nếu đã gửi)

2. **Trang "Quản lý Shop"** (`/seller/shop`)
   - Chỉ hiển thị khi user có role SELLER
   - Xem/chỉnh sửa thông tin shop
   - Quản lý sản phẩm
   - Quản lý đơn hàng

### 6.2. Admin Pages

1. **Trang "Quản lý Yêu cầu Seller"** (`/admin/seller-requests`)
   - Danh sách requests
   - Filter theo status
   - Xem chi tiết và duyệt/từ chối

---

## 7. Security & Validation

### 7.1. Authorization

- **Tạo request:** Chỉ user có role `ROLE_BUYER` (chưa có `ROLE_SELLER`)
- **Xem request:** User chỉ xem được request của chính mình
- **Duyệt request:** Chỉ user có role `ROLE_ADMIN`

### 7.2. Validation

- Tên shop: 3-100 ký tự, unique
- Mô tả: Tối đa 2000 ký tự
- Số điện thoại: Format hợp lệ
- Địa chỉ: Không được để trống
- Logo: Bắt buộc, file ảnh, max 5MB
- Ảnh bìa: Optional, file ảnh, max 10MB

### 7.3. Business Rules

- Một user chỉ có một request đang pending
- Không thể tạo request mới nếu đã có shop
- Không thể reject request đã được approve

---

## 8. Edge Cases & Error Handling

### 8.1. Trường Hợp Đặc Biệt

1. **User đã có role SELLER:**
   - Không cho tạo request mới
   - Hiển thị thông báo: "Bạn đã là seller"

2. **User đã có shop:**
   - Không cho tạo request mới
   - Redirect đến trang quản lý shop

3. **Request bị reject:**
   - User có thể xem lý do
   - User có thể chỉnh sửa và gửi lại

4. **Request đang pending:**
   - Không cho tạo request mới
   - Hiển thị trạng thái "Đang chờ duyệt"

5. **Admin reject nhưng không điền lý do:**
   - Yêu cầu bắt buộc điền lý do

---

## 9. Implementation Priority

### Phase 1: Core Features (Ưu tiên cao)
1. ✅ Entity `SellerRequest`
2. ✅ Repository `SellerRequestRepository`
3. ✅ Service `SellerRequestService`
4. ✅ Controller cho user (tạo request, xem status)
5. ✅ Controller cho admin (duyệt/từ chối)
6. ✅ Frontend: Trang "Trở thành Seller"
7. ✅ Frontend: Trang admin quản lý requests

### Phase 2: Enhancements (Ưu tiên trung bình)
1. Email notification khi request được duyệt/từ chối
2. Upload và lưu logo/ảnh bìa vào MinIO
3. Preview shop trước khi submit
4. Lịch sử requests (nếu user gửi lại sau khi bị reject)

### Phase 3: Advanced Features (Ưu tiên thấp)
1. Xác thực CMND/CCCD
2. Yêu cầu giấy phép kinh doanh
3. Phí đăng ký seller (nếu có)
4. Thống kê số lượng requests

---

## 10. Database Migration

### 10.1. Tạo Bảng `seller_requests`

```sql
CREATE TABLE seller_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_name VARCHAR(255) NOT NULL,
    shop_description TEXT,
    shop_phone VARCHAR(20),
    shop_address VARCHAR(255),
    logo_url VARCHAR(255),
    cover_image_url VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_pending_request UNIQUE (user_id, status) 
        WHERE status = 'PENDING'
);
```

### 10.2. Cập Nhật Bảng `shops`

```sql
-- Đảm bảo user_id unique
ALTER TABLE shops ADD CONSTRAINT unique_user_shop UNIQUE (user_id);
```

---

## 11. Kết Luận

### 11.1. Giải Pháp Đề Xuất

**Quy trình có duyệt (Option 2)** là phù hợp nhất vì:
- Đảm bảo chất lượng seller
- Kiểm soát được gian hàng trên sàn
- Có thể mở rộng thêm validation sau này

### 11.2. Next Steps

1. Tạo entity `SellerRequest`
2. Implement service layer
3. Tạo API endpoints
4. Xây dựng frontend
5. Test và deploy

---

## 12. Questions & Considerations

1. **Có cần phí đăng ký seller không?**
   - Nếu có → Cần tích hợp payment gateway

2. **Có cần xác thực danh tính không?**
   - CMND/CCCD
   - Giấy phép kinh doanh
   - → Cần thêm bảng lưu documents

3. **Có cho phép user vừa là buyer vừa là seller không?**
   - Hiện tại: Có (user có thể có cả 2 roles)
   - → Cần UI phân biệt rõ ràng

4. **Có cho phép user đổi tên shop sau khi được duyệt không?**
   - Nên có giới hạn số lần đổi
   - Cần admin duyệt nếu đổi tên

5. **Có cần thời gian chờ giữa các lần gửi request không?**
   - Tránh spam requests
   - Ví dụ: 30 ngày sau khi bị reject mới được gửi lại
