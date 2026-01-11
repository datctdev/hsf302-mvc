# Tài Khoản Mặc Định

Khi chạy ứng dụng lần đầu, hệ thống sẽ tự động tạo 3 tài khoản mặc định để test.

## 📋 Danh Sách Tài Khoản

### 1. Tài Khoản BUYER (Người Mua)
- **Email**: `buyer@gmail.com`
- **Mật khẩu**: `buyer123@`
- **Vai trò**: `ROLE_BUYER`
- **Họ tên**: Người Mua
- **Quyền**: Xem sản phẩm, thêm vào giỏ hàng, đặt hàng, đánh giá

### 2. Tài Khoản SELLER (Người Bán)
- **Email**: `seller@gmail.com`
- **Mật khẩu**: `seller123@`
- **Vai trò**: `ROLE_SELLER`
- **Họ tên**: Người Bán
- **Quyền**: Tất cả quyền của BUYER + Quản lý gian hàng, sản phẩm, đơn hàng của shop

### 3. Tài Khoản ADMIN (Quản Trị Viên)
- **Email**: `admin@gmail.com`
- **Mật khẩu**: `admin123@`
- **Vai trò**: `ROLE_ADMIN`
- **Họ tên**: Quản Trị Viên
- **Quyền**: Toàn quyền trên hệ thống

## 🔐 Pattern Mật Khẩu

Tất cả tài khoản mặc định đều sử dụng pattern:
- **Email**: `{role}@gmail.com` (ví dụ: buyer@gmail.com, seller@gmail.com, admin@gmail.com)
- **Mật khẩu**: `{role}123@` (ví dụ: buyer123@, seller123@, admin123@)

## ⚠️ Lưu Ý

1. **Bảo mật**: Các tài khoản này chỉ nên sử dụng trong môi trường development/test
2. **Production**: Trong môi trường production, nên xóa hoặc vô hiệu hóa các tài khoản mặc định này
3. **Khởi tạo**: Các tài khoản chỉ được tạo nếu chưa tồn tại (kiểm tra theo email)

## 🚀 Sử Dụng

Bạn có thể đăng nhập với bất kỳ tài khoản nào ở trên để test các chức năng tương ứng với vai trò của tài khoản đó.

---

**Cập nhật**: 11-01-2026
