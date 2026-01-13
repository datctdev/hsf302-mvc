# API Documentation

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026  
**Base URL:** `http://localhost:8080/api`

---

## 🔐 Authentication

Tất cả API endpoints (trừ `/api/auth/register`, `/api/auth/login`) đều yêu cầu JWT token trong header:

```
Authorization: Bearer {jwt_token}
```

### Refresh Token
Nếu token hết hạn (401), client có thể tự động refresh token bằng endpoint `/api/auth/refresh`.

---

## 📋 Endpoints

### 🔑 Authentication Module (`/api/auth`)

#### 1. Đăng Ký
- **POST** `/api/auth/register`
- **Auth:** Không cần
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0123456789"
}
```
- **Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "uuid-refresh-token",
  "roles": ["ROLE_BUYER"]
}
```

#### 2. Đăng Nhập
- **POST** `/api/auth/login`
- **Auth:** Không cần
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "rememberMe": true
}
```
- **Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "uuid-refresh-token",
  "roles": ["ROLE_BUYER"]
}
```

#### 3. Refresh Token
- **POST** `/api/auth/refresh`
- **Auth:** Không cần
- **Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```
- **Response:** `200 OK`
```json
{
  "token": "new-jwt-token",
  "refreshToken": "new-refresh-token"
}
```

#### 4. Lấy Thông Tin User Hiện Tại
- **GET** `/api/auth/me`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0123456789",
  "avatarUrl": "http://minio:9000/bucket/avatars/avatar.jpg",
  "roles": ["ROLE_BUYER"],
  "isActive": true,
  "createdAt": "2026-01-11T10:00:00"
}
```

#### 5. Đổi Mật Khẩu
- **POST** `/api/auth/change-password`
- **Auth:** Required
- **Request Body:**
```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewPassword123",
  "confirmPassword": "NewPassword123"
}
```
- **Response:** `200 OK`
```json
{
  "message": "Mật khẩu đã được thay đổi thành công"
}
```

#### 6. Cập Nhật Profile
- **PUT** `/api/auth/profile`
- **Auth:** Required
- **Request Body:**
```json
{
  "fullName": "Nguyễn Văn B",
  "phoneNumber": "0987654321",
  "avatarUrl": "http://minio:9000/bucket/avatars/new-avatar.jpg"
}
```
- **Response:** `200 OK`
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "fullName": "Nguyễn Văn B",
  "phoneNumber": "0987654321",
  "avatarUrl": "http://minio:9000/bucket/avatars/new-avatar.jpg",
  "roles": ["ROLE_BUYER"],
  "isActive": true,
  "createdAt": "2026-01-11T10:00:00"
}
```

#### 7. Vô Hiệu Hóa Tài Khoản
- **POST** `/api/auth/deactivate`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "message": "Tài khoản đã được vô hiệu hóa"
}
```

#### 8. Kích Hoạt Tài Khoản
- **POST** `/api/auth/activate`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "message": "Tài khoản đã được kích hoạt"
}
```

#### 9. Đăng Xuất
- **POST** `/api/auth/logout`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "message": "Đăng xuất thành công"
}
```

---

### 🏪 Seller Module (`/api/seller`)

#### 1. Kiểm Tra Trạng Thái Seller
- **GET** `/api/seller/check`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "message": "Bạn đã là seller" | "Bạn chưa là seller" | "Yêu cầu đang chờ duyệt"
}
```

#### 2. Tạo Yêu Cầu Trở Thành Seller
- **POST** `/api/seller/request`
- **Auth:** Required
- **Request Body:**
```json
{
  "shopName": "Shop ABC",
  "shopDescription": "Mô tả shop",
  "shopPhone": "0123456789",
  "shopAddress": "123 Đường ABC, Quận XYZ",
  "logoUrl": "http://minio:9000/bucket/shop-logos/logo.jpg",
  "coverImageUrl": "http://minio:9000/bucket/shop-covers/cover.jpg"
}
```
- **Response:** `201 Created`
```json
{
  "id": "uuid",
  "shopName": "Shop ABC",
  "status": "PENDING",
  "createdAt": "2026-01-11T10:00:00"
}
```

#### 3. Cập Nhật Yêu Cầu Seller
- **PUT** `/api/seller/request`
- **Auth:** Required
- **Request Body:** (giống POST)
- **Response:** `200 OK`

#### 4. Hủy Yêu Cầu Seller
- **DELETE** `/api/seller/request`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "message": "Đã hủy request thành công"
}
```

#### 5. Lấy Trạng Thái Request
- **GET** `/api/seller/request/status`
- **Auth:** Required
- **Response:** `200 OK`
```json
{
  "id": "uuid",
  "shopName": "Shop ABC",
  "status": "PENDING",
  "rejectionReason": null,
  "createdAt": "2026-01-11T10:00:00"
}
```

---

### 🛒 Shop Module (`/api/shop`)

#### 1. Lấy Thông Tin Shop
- **GET** `/api/shop`
- **Auth:** Required (SELLER)
- **Response:** `200 OK`
```json
{
  "id": "uuid",
  "name": "Shop ABC",
  "description": "Mô tả shop",
  "logoUrl": "http://minio:9000/bucket/shop-logos/logo.jpg",
  "coverImageUrl": "http://minio:9000/bucket/shop-covers/cover.jpg",
  "phoneNumber": "0123456789",
  "address": "123 Đường ABC",
  "status": "ACTIVE",
  "averageRating": 4.5,
  "createdAt": "2026-01-11T10:00:00"
}
```

#### 2. Cập Nhật Thông Tin Shop
- **PUT** `/api/shop`
- **Auth:** Required (SELLER)
- **Request Body:**
```json
{
  "name": "Shop ABC Updated",
  "description": "Mô tả mới",
  "phoneNumber": "0987654321",
  "address": "456 Đường XYZ",
  "logoUrl": "http://minio:9000/bucket/shop-logos/new-logo.jpg",
  "coverImageUrl": "http://minio:9000/bucket/shop-covers/new-cover.jpg"
}
```
- **Response:** `200 OK` (giống GET)

---

### 👨‍💼 Admin Module (`/api/admin`)

#### 1. Lấy Danh Sách Yêu Cầu Seller
- **GET** `/api/admin/seller-requests`
- **Query Params:** `?status=PENDING|APPROVED|REJECTED`
- **Auth:** Required (ADMIN)
- **Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "shopName": "Shop ABC",
    "shopDescription": "Mô tả",
    "shopPhone": "0123456789",
    "shopAddress": "123 Đường ABC",
    "logoUrl": "http://...",
    "coverImageUrl": "http://...",
    "status": "PENDING",
    "rejectionReason": null,
    "createdAt": "2026-01-11T10:00:00",
    "reviewedAt": null
  }
]
```

#### 2. Duyệt Yêu Cầu Seller
- **POST** `/api/admin/seller-requests/{requestId}/approve`
- **Auth:** Required (ADMIN)
- **Response:** `200 OK`
```json
{
  "message": "Đã duyệt yêu cầu thành công"
}
```

#### 3. Từ Chối Yêu Cầu Seller
- **POST** `/api/admin/seller-requests/{requestId}/reject`
- **Auth:** Required (ADMIN)
- **Request Body:**
```json
{
  "rejectionReason": "Lý do từ chối"
}
```
- **Response:** `200 OK`
```json
{
  "message": "Đã từ chối yêu cầu"
}
```

---

### 📁 File Module (`/api/files`)

#### 1. Upload File
- **POST** `/api/files/upload`
- **Auth:** Required
- **Content-Type:** `multipart/form-data`
- **Form Data:**
  - `file`: File (image)
  - `folder`: String (ví dụ: "avatars", "shop-logos", "shop-covers")
- **Response:** `200 OK`
```json
{
  "url": "http://minio:9000/bucket/avatars/filename.jpg",
  "filename": "filename.jpg",
  "size": 1024000,
  "contentType": "image/jpeg"
}
```

---

## ❌ Error Responses

Tất cả errors đều trả về format:

```json
{
  "timestamp": "2026-01-11T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Thông báo lỗi",
  "path": "/api/auth/register"
}
```

### Common Error Codes:
- **400 Bad Request:** Validation errors
- **401 Unauthorized:** Token không hợp lệ hoặc hết hạn
- **403 Forbidden:** Không có quyền truy cập
- **404 Not Found:** Resource không tồn tại
- **409 Conflict:** Email đã tồn tại
- **500 Internal Server Error:** Lỗi server

### Validation Errors:
```json
{
  "timestamp": "2026-01-11T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải có ít nhất 8 ký tự"
  },
  "path": "/api/auth/register"
}
```

---

## 📝 Notes

1. **JWT Token Expiry:** Token có thời hạn, cần refresh khi hết hạn
2. **File Upload:** Max size 25MB, chỉ chấp nhận images
3. **Pagination:** Các API list sẽ có pagination trong tương lai
4. **Rate Limiting:** Chưa implement, sẽ thêm sau

---

**Cập nhật:** 11-01-2026
