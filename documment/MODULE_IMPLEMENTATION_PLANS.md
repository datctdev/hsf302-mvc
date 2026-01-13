# Module Implementation Plans

**Phiên bản:** 1.0  
**Ngày:** 11-01-2026

---

## 📋 Tổng Quan

Tài liệu này mô tả kế hoạch implement các module còn lại của dự án E-commerce.

### ✅ Modules Đã Hoàn Thành

1. **Auth Module** ✅
   - Register, Login, Logout
   - JWT Authentication
   - Profile Management
   - Change Password
   - Account Activation/Deactivation

2. **Seller Module** ✅
   - Become Seller Request
   - Admin Approval/Rejection
   - Seller Status Check

3. **Shop Module** ✅
   - Shop Creation (tự động khi approve seller)
   - Shop Information Management
   - Shop Status Management

4. **File Module** ✅
   - File Upload (MinIO)
   - Avatar Upload
   - Shop Logo/Cover Upload

---

## 🚧 Modules Cần Implement

### 1. Product Management Module (F-PRODUCT)

#### 1.1. Requirements

**Seller:**
- Thêm sản phẩm mới
- Chỉnh sửa sản phẩm
- Xóa sản phẩm
- Quản lý variants (size, color, etc.)
- Upload ảnh sản phẩm
- Quản lý inventory (stock)

**Buyer:**
- Xem danh sách sản phẩm
- Tìm kiếm sản phẩm
- Lọc sản phẩm (category, price, shop)
- Xem chi tiết sản phẩm
- Xem ảnh sản phẩm

**Admin:**
- Xem tất cả sản phẩm
- Duyệt/Xóa sản phẩm
- Quản lý categories

#### 1.2. Database Changes

**Tables cần tạo:**
- `products` (đã có trong schema)
- `product_variants` (đã có)
- `product_images` (đã có)
- `product_categories` (đã có)
- `product_category_mappings` (nếu cần many-to-many)

#### 1.3. API Endpoints

**Seller:**
- `POST /api/seller/products` - Tạo sản phẩm
- `GET /api/seller/products` - Danh sách sản phẩm của seller
- `GET /api/seller/products/{id}` - Chi tiết sản phẩm
- `PUT /api/seller/products/{id}` - Cập nhật sản phẩm
- `DELETE /api/seller/products/{id}` - Xóa sản phẩm

**Buyer:**
- `GET /api/products` - Danh sách sản phẩm (public)
- `GET /api/products/{id}` - Chi tiết sản phẩm
- `GET /api/products/search` - Tìm kiếm sản phẩm
- `GET /api/products/categories` - Danh sách categories

**Admin:**
- `GET /api/admin/products` - Tất cả sản phẩm
- `PUT /api/admin/products/{id}/approve` - Duyệt sản phẩm
- `DELETE /api/admin/products/{id}` - Xóa sản phẩm

#### 1.4. Frontend Pages

**Seller:**
- `/seller/products` - Danh sách sản phẩm
- `/seller/products/new` - Tạo sản phẩm mới
- `/seller/products/{id}/edit` - Chỉnh sửa sản phẩm

**Buyer:**
- `/products` - Danh sách sản phẩm
- `/products/{id}` - Chi tiết sản phẩm

**Admin:**
- `/admin/products` - Quản lý sản phẩm

#### 1.5. Implementation Steps

1. ✅ Tạo entities (Product, ProductVariant, ProductImage, ProductCategory)
2. ✅ Tạo repositories
3. ✅ Tạo DTOs (request/response)
4. ✅ Tạo services (ProductService)
5. ✅ Tạo controllers (SellerProductController, ProductController, AdminProductController)
6. ⏳ Tạo frontend pages
7. ⏳ Implement search và filter
8. ⏳ Implement image upload

---

### 2. Cart Management Module (F-CART)

#### 2.1. Requirements

**Buyer:**
- Thêm sản phẩm vào giỏ hàng
- Xem giỏ hàng
- Cập nhật số lượng
- Xóa sản phẩm khỏi giỏ hàng
- Tính tổng tiền

#### 2.2. Database Changes

**Tables cần tạo:**
- `carts` (đã có trong schema)
- `cart_items` (đã có)

#### 2.3. API Endpoints

- `GET /api/cart` - Lấy giỏ hàng
- `POST /api/cart/items` - Thêm sản phẩm vào giỏ
- `PUT /api/cart/items/{id}` - Cập nhật số lượng
- `DELETE /api/cart/items/{id}` - Xóa sản phẩm
- `DELETE /api/cart` - Xóa toàn bộ giỏ hàng

#### 2.4. Frontend Pages

- `/cart` - Trang giỏ hàng

#### 2.5. Implementation Steps

1. ⏳ Tạo entities (Cart, CartItem)
2. ⏳ Tạo repositories
3. ⏳ Tạo DTOs
4. ⏳ Tạo services (CartService)
5. ⏳ Tạo controllers (CartController)
6. ⏳ Tạo frontend page
7. ⏳ Implement real-time updates (optional)

---

### 3. Order Management Module (F-ORDER)

#### 3.1. Requirements

**Buyer:**
- Tạo đơn hàng từ giỏ hàng
- Xem danh sách đơn hàng
- Xem chi tiết đơn hàng
- Hủy đơn hàng (nếu chưa ship)

**Seller:**
- Xem đơn hàng của shop
- Cập nhật trạng thái đơn hàng
- In hóa đơn

**Admin:**
- Xem tất cả đơn hàng
- Quản lý đơn hàng
- Thống kê đơn hàng

#### 3.2. Database Changes

**Tables cần tạo:**
- `orders` (master order)
- `order_items` (sub-orders per shop)
- `order_status_history` (tracking)

#### 3.3. API Endpoints

**Buyer:**
- `POST /api/orders` - Tạo đơn hàng
- `GET /api/orders` - Danh sách đơn hàng
- `GET /api/orders/{id}` - Chi tiết đơn hàng
- `PUT /api/orders/{id}/cancel` - Hủy đơn hàng

**Seller:**
- `GET /api/seller/orders` - Đơn hàng của shop
- `PUT /api/seller/orders/{id}/status` - Cập nhật trạng thái

**Admin:**
- `GET /api/admin/orders` - Tất cả đơn hàng
- `GET /api/admin/orders/statistics` - Thống kê

#### 3.4. Frontend Pages

**Buyer:**
- `/orders` - Danh sách đơn hàng
- `/orders/{id}` - Chi tiết đơn hàng

**Seller:**
- `/seller/orders` - Đơn hàng của shop

**Admin:**
- `/admin/orders` - Quản lý đơn hàng

#### 3.5. Implementation Steps

1. ⏳ Tạo entities (Order, OrderItem, OrderStatus)
2. ⏳ Tạo repositories
3. ⏳ Tạo DTOs
4. ⏳ Tạo services (OrderService)
5. ⏳ Implement order splitting logic (master-sub orders)
6. ⏳ Tạo controllers
7. ⏳ Tạo frontend pages

---

### 4. Payment Integration Module (F-PAYMENT)

#### 4.1. Requirements

- Tích hợp VNPay
- Tích hợp Momo
- Xử lý payment callbacks
- Lưu payment history

#### 4.2. Database Changes

**Tables cần tạo:**
- `payments` (payment records)
- `payment_methods` (VNPay, Momo)

#### 4.3. API Endpoints

- `POST /api/payments/create` - Tạo payment request
- `POST /api/payments/callback` - Payment callback
- `GET /api/payments/{id}` - Chi tiết payment

#### 4.4. Implementation Steps

1. ⏳ Tạo entities
2. ⏳ Tích hợp VNPay SDK
3. ⏳ Tích hợp Momo SDK
4. ⏳ Tạo services (PaymentService)
5. ⏳ Tạo controllers
6. ⏳ Implement callback handling
7. ⏳ Update order status after payment

---

### 5. Review System Module (F-REVIEW)

#### 5.1. Requirements

**Buyer:**
- Đánh giá sản phẩm sau khi mua
- Đánh giá shop
- Xem đánh giá của người khác

**Seller:**
- Xem đánh giá shop
- Phản hồi đánh giá

#### 5.2. Database Changes

**Tables cần tạo:**
- `product_reviews` (đánh giá sản phẩm)
- `shop_reviews` (đánh giá shop)

#### 5.3. API Endpoints

- `POST /api/reviews/products/{productId}` - Đánh giá sản phẩm
- `GET /api/reviews/products/{productId}` - Xem đánh giá sản phẩm
- `POST /api/reviews/shops/{shopId}` - Đánh giá shop
- `GET /api/reviews/shops/{shopId}` - Xem đánh giá shop

#### 5.4. Implementation Steps

1. ⏳ Tạo entities
2. ⏳ Tạo repositories
3. ⏳ Tạo DTOs
4. ⏳ Tạo services (ReviewService)
5. ⏳ Tạo controllers
6. ⏳ Tính toán average rating
7. ⏳ Tạo frontend pages

---

### 6. AI Integration Module (F-AI)

#### 6.1. Requirements

- Phân loại sản phẩm tự động (AI)
- Chatbot hỗ trợ khách hàng

#### 6.2. Implementation Steps

1. ⏳ Tạo AI service (Python/FastAPI)
2. ⏳ Tích hợp với Spring Boot
3. ⏳ Implement product classification
4. ⏳ Integrate chatbot widget

---

## 📅 Implementation Priority

### Phase 1 (High Priority)
1. ✅ Product Management Module
2. ✅ Cart Management Module
3. ✅ Order Management Module

### Phase 2 (Medium Priority)
4. ✅ Payment Integration Module
5. ✅ Review System Module

### Phase 3 (Low Priority)
6. ✅ AI Integration Module

---

## 📝 Notes

- Mỗi module nên được implement độc lập
- Test từng module trước khi chuyển sang module tiếp theo
- Update API documentation sau mỗi module
- Update frontend guide nếu có thay đổi architecture

---

**Cập nhật:** 11-01-2026
