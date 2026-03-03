# Luồng Checkout, Thanh toán, GHN, Hoa hồng và Tiền shop

## 1. Tổng quan công thức tiền

- **Buyer trả:** `order.total = subtotal + shippingFee`
  - `subtotal`: tổng tiền hàng (theo đơn từng shop).
  - `shippingFee`: phí vận chuyển GHN **của đơn đó** (mỗi đơn = 1 shop = 1 phí GHN riêng).

- **Nền tảng (commission):** Hoa hồng tính trên **subtotal** (tiền hàng), không tính trên shipping.
  - `platformCommission = sum( itemSubtotal × commissionRate% )` theo từng dòng sản phẩm (rate theo category).
  - Commission được **tạo khi đơn chuyển DELIVERED** (giao hàng thành công).

- **Shop nhận (sau khi đã trừ hoa hồng):**
  - Doanh thu đơn = `order.total` (buyer đã trả).
  - Hoa hồng nền tảng trừ trên tiền hàng: `order.platformCommission`.
  - Shop thực tế: `(subtotal - platformCommission) + shippingFee`.
  - Khi tạo đơn GHN: **payment_type_id = 1** → **người gửi (shop) trả phí cho GHN**.  
  → Phí ship buyer trả trong `order.total` về tay shop; shop dùng phần đó (hoặc tự bù) để trả GHN. Nếu phí ship trong đơn = phí GHN thực tế thì shop không lỗ phần ship.

---

## 2. Checkout (tạo đơn)

### 2.1 Một shop (đặt 1 shop)

- User chọn địa chỉ (Tỉnh / Quận / Phường) → gọi API tính phí **một shop** → hiển thị phí + tổng.
- Form gửi: `shopId`, địa chỉ, `shippingFee` (đã tính cho shop đó).
- Backend: `createOrder()` → 1 đơn, `order.shippingFee = request.getShippingFee()`.

### 2.2 Nhiều shop (Đặt tất cả)

- User chọn địa chỉ → gọi API **tính phí theo từng shop** (phí GHN từng shop → địa chỉ giao hàng), **tổng phí = tổng các phí shop**.
- Form gửi: không `shopId`, địa chỉ (`shippingDistrictId`, `shippingWardCode`). **Không** gửi một con `shippingFee` tổng rồi chia đều.
- Backend: `createOrdersFromCart()`:
  - Nhóm giỏ theo shop.
  - **Mỗi shop:** tính phí GHN riêng: từ địa chỉ shop → địa chỉ giao hàng, cân nặng = tổng cân nặng **sản phẩm của shop đó**.
  - Mỗi đơn: `order.shippingFee = phí GHN của đúng shop đó`.
  - `order.total = order.subtotal + order.shippingFee` (đã đúng từng đơn).

**Lưu ý:** Nếu dùng một phí chung (ví dụ phí 1 shop) rồi chia đều cho N shop thì sai: mỗi shop có khoảng cách/địa chỉ khác nhau → phí GHN khác nhau.

---

## 3. Thanh toán (transaction)

### 3.1 Một đơn

- COD: Buyer xác nhận COD → đơn chuyển CONFIRMED, trừ tồn kho, tạo commission **khi DELIVERED**.
- VNPay: Tạo 1 Payment (1-1 với Order) → redirect VNPay → callback → CONFIRMED, trừ tồn kho, xóa giỏ (theo shop đơn đó).

### 3.2 Nhiều đơn (batch)

- Sau khi tạo N đơn (N shop) → redirect `/payments/batch?orderIds=...`.
- Trang batch: hiển thị tổng tiền = tổng `order.total` của N đơn.
- User bấm "Thanh toán qua VNPay (1 lượt)" → tạo **một BatchPayment** (tổng tiền, `transactionId` dạng `B-...`) → redirect VNPay **một lần**.
- Callback VNPay (vnp_TxnRef bắt đầu `B-`): cập nhật BatchPayment, với mỗi đơn trong batch: CONFIRMED, trừ tồn kho, xóa giỏ theo từng shop → redirect `/orders`.

---

## 4. GHN (Giao Hàng Nhanh)

- **Tạo đơn GHN:** Khi đơn chuyển CONFIRMED (từ PENDING_PAYMENT) hoặc khi chuyển PROCESSING/SHIPPING (từ CONFIRMED).  
  Dùng: `order.shippingName/Phone/Address`, `shippingDistrictId`, `shippingWardCode`, cân nặng từ order items, địa chỉ **shop** (from), **payment_type_id = 1** (shop trả phí).
- **Tiền ship:** Buyer đã trả `order.shippingFee` trong `order.total`. Shop là người trả GHN; phần tiền ship buyer trả sẽ về shop (qua luồng thanh toán của bạn) để shop dùng trả GHN. Cần đảm bảo `order.shippingFee` = phí GHN thực tế của đơn đó (tính đúng theo shop + địa chỉ giao).

---

## 5. Hoa hồng nền tảng

- **Tạo commission:** Khi đơn chuyển trạng thái **DELIVERED** (giao hàng thành công) – trong `updateOrderStatus` hoặc GHN webhook `markDeliveredByGhnRef`.
- **Dữ liệu:** Bảng `commissions`: `orderId`, `sellerId`, `orderAmount` (= `order.subtotal`), `totalCommission`.  
  Chi tiết theo dòng: `commission_items`: theo từng OrderItem, `commissionRate` (theo category), `commissionAmount` = tiền hàng dòng đó × rate.
- **Ý nghĩa:** Nền tảng thu hoa hồng trên **tiền hàng** (subtotal), không thu trên phí ship.

---

## 6. Tiền của shop

- **Doanh thu (revenue):** Thường lấy từ các đơn **DELIVERED**, ví dụ `SUM(order.total)` hoặc theo logic thống kê (revenue = tiền đã giao).
- **Sau hoa hồng:** Shop nhận từ đơn = `order.total` (đã thu từ buyer) trừ đi phần nền tảng thu: hiệu quả là `(subtotal - platformCommission) + shippingFee`.  
  Phí GHN shop tự trả (payment_type_id = 1); nếu `order.shippingFee` khớp với phí GHN thì phần ship cân bằng.

---

## 7. Ràng buộc cần đảm bảo

1. **Phí ship đa shop:** Luôn tính phí GHN **theo từng shop** (từ shop → địa chỉ giao, cân nặng theo sản phẩm shop đó). Không dùng một phí rồi chia đều.
2. **Checkout:** API tính phí cho cart (nhiều shop) trả về tổng phí + (tùy chọn) phí từng shop; form chỉ gửi địa chỉ; backend tạo đơn时 tự tính lại phí từng shop và gán đúng `order.shippingFee`.
3. **Batch payment:** Một lần thanh toán VNPay cho nhiều đơn; callback cập nhật đúng từng đơn và xóa giỏ theo từng shop.
4. **Commission:** Chỉ tạo khi DELIVERED; `orderAmount` = subtotal; rate theo category.
5. **GHN:** payment_type_id = 1 (shop trả); `order.shippingFee` phản ánh đúng phí GHN của đơn đó.
