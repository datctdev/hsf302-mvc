# Phân Tích Shipping Fee & Hệ Thống Vận Chuyển

## 1. Hiện trạng (Current State)

### 1.1 Cách thu phí ship hiện tại

| Thành phần | Chi tiết |
|------------|----------|
| **Checkout** | Ô input **Phí Vận Chuyển (VNĐ)** — user **tự nhập** số tiền (`min=0`, `step=1000`, `value=0`). |
| **CreateOrderRequest** | `shippingFee` (bắt buộc, mặc định 0). |
| **Order** | Lưu `shippingFee`, `total = subtotal + shippingFee`. |
| **Tính toán** | **Không có** — không gọi API, không có bảng giá, không có công thức. |

→ Khách có thể nhập 0, nhập sai, không phản ánh phí thực tế. Seller cũng không có công cụ để ước tính hoặc đối soát.

---

### 1.2 Dữ liệu sẵn có (để tính phí / gọi API)

| Nguồn | Dữ liệu | Ghi chú |
|-------|---------|---------|
| **Shop** | `address` (1 trường text) | Có thể dùng làm **điểm lấy hàng**. Chưa có `ward`, `district`, `city` tách riêng — khó map chuẩn với API (GHN/GHTK dùng mã ward/district). |
| **Order (destination)** | `shippingAddress`, `shippingCity`, `shippingDistrict`, `shippingWard` | **Điểm giao hàng**. Đủ để tra cứu phí nếu có bảng giá theo vùng hoặc API cần ward/district. |
| **Product / OrderItem** | Không có `weight`, `length`, `width`, `height` | Hầu hết API vận chuyển cần **cân nặng** (và có thể kích thước) để tính phí. Hiện chưa có. |

→ Để dùng API bên thứ 3 hoặc bảng giá theo vùng, cần **bổ sung**: địa chỉ Shop/Order chuẩn hóa (ward/district/city), và **cân nặng** sản phẩm (ít nhất).

---

## 2. Ba Hướng Chính

---

## Phương Án A: Gọi API Bên Thứ 3 (GHN, GHTK, Viettel Post, …)

### 2.1 Ý tưởng

- Dùng **API của đơn vị vận chuyển** để:
  1. **Tra cứu phí** (calculate fee / get rate): từ điểm lấy hàng → điểm giao, theo cân nặng (và có thể kích thước), loại dịch vụ (chuẩn, nhanh, …).
  2. **Tạo đơn vận chuyển** (create order / create shipment): sau khi đơn hàng được xác nhận, gọi API tạo đơn với đối tác → họ đến lấy hàng và giao.
  3. **Tra cứu vận đơn** (tracking): theo mã vận đơn.

- **Bạn không** quản lý shipper, không cần đội giao hàng riêng. Đối tác lo toàn bộ: lấy hàng, vận chuyển, giao.

### 2.2 Một số đơn vị phổ biến tại Việt Nam

| Đơn vị | API | Tính năng chính | Ghi chú |
|--------|-----|-----------------|---------|
| **Giao Hàng Nhanh (GHN)** | Có, REST | Tính phí, tạo đơn, tracking, bảng giá theo km/kg | Phổ biến, tài liệu rõ, có sandbox. |
| **Giao Hàng Tiết Kiệm (GHTK)** | Có, REST | Tính phí, tạo đơn, tracking | Giá cạnh tranh, nhiều shop dùng. |
| **Viettel Post** | Có | Tính phí, tạo đơn, tracking | Mạng lưới rộng. |
| **J&T Express** | Có | Tạo đơn, tracking | Ít thấy API tính phí công khai cho đối tác nhỏ. |
| **Ninja Van** | Có | Tương tự | |
| **Lalamove** (vận chuyển nhanh, xe máy/ô tô) | Có | Ước tính phí, đặt xe | Phù hợp giao gấp, nội thành. |

→ Thực tế, **GHN** và **GHTK** hay được tích hợp nhất cho e-commerce vừa và nhỏ: API ổn, có tính phí + tạo đơn.

### 2.3 Đầu vào API tính phí (ví dụ GHN / GHTK)

- **Điểm đi (pickup):**  
  - Mã **ward** (phường/xã) hoặc **district** (quận/huyện) + **province** (tỉnh/thành).  
  - Hoặc địa chỉ chi tiết — tùy API, nhiều API yêu cầu **mã** để tính chính xác.
- **Điểm đến (delivery):**  
  - Tương tự: ward/district + province (hoặc mã).
- **Cân nặng:** kg (thường bắt buộc). Một số API có thêm **kích thước** (dài, rộng, cao) cho hàng cồng kềnh.
- **Dịch vụ:** chuẩn, nhanh, … (tùy đơn vị).

→ **Shop** và **địa chỉ giao hàng** cần **map được sang mã ward/district/province** (bảng địa chỉ Việt Nam). Nhiều đơn vị cung cấp file/mã địa chỉ; có thể dùng chung hoặc theo từng API.

### 2.4 Ưu điểm

- **Không cần** quản lý shipper, xe, tuyến, bảo hiểm vận chuyển.
- **Phí ship** tính theo bảng giá thực tế của đối tác → sát thị trường, minh bạch cho khách.
- **Tạo đơn vận chuyển** và **tracking** gắn trực tiếp với đơn hàng → seller và buyer đều theo dõi được.
- Thời gian tích hợp **hợp lý** (vài tuần cho 1 đơn vị, sau đó mở rộng thêm).
- Có **sandbox** (GHN, GHTK) để test.

### 2.5 Nhược điểm & ràng buộc

- **Phụ thuộc** đối tác: API lỗi, đổi format, đổi chính sách giá.
- **Chuẩn hóa địa chỉ**:  
  - Shop: cần `ward`, `district`, `province` (hoặc mã) thay vì chỉ 1 ô `address`.  
  - Địa chỉ giao hàng: đã có `ward`/`district`/`city` nhưng có thể cần **chuẩn hóa theo mã** (dropdown chọn tỉnh → quận → phường) để gọi API chính xác.
- **Cân nặng sản phẩm**:  
  - Cần thêm `weight` (và có thể `length`, `width`, `height`) cho **Product** hoặc **ProductVariant**.  
  - Đơn hàng: tổng weight = tổng (weight × quantity) của từng item.
- **Hợp đồng & tài khoản**:  
  - Mỗi Shop (hoặc nền tảng) cần **đăng ký, ký hợp đồng** với GHN/GHTK/…, có **Token/API Key**.  
  - Có thể: 1 tài khoản nền tảng dùng chung cho mọi Shop, hoặc mỗi Shop tự kết nối (phức tạp hơn).
- **Giá có thể thay đổi**: phí API trả về là **ước tính**; khi tạo đơn thực tế đối tác có thể điều chỉnh (thường nhỏ). Cần quy định: lấy giá lúc đặt hàng hay lúc tạo vận đơn.

### 2.6 Công việc kỹ thuật (nếu chọn A)

- Bổ sung **weight** (và tùy chọn kích thước) cho Product/Variant.
- Chuẩn hóa **địa chỉ Shop** (ward, district, province hoặc mã) và **địa chỉ giao hàng** (dropdown hoặc autocomplete theo bảng mã).
- **ShippingService** (hoặc GHN/GHTK client):
  - `calculateFee(pickupWardId, deliveryWardId, weightKg, serviceType)` → gọi API, trả về phí (và thời gian dự kiến nếu có).
- Ở **checkout**:  
  - Khi đã có địa chỉ giao + giỏ hàng (→ weight), gọi `calculateFee` (có thể qua REST API của bạn cho frontend, hoặc tính ở backend khi load checkout).  
  - Hiển thị **phí ship** (và có thể vài phương án: chuẩn, nhanh).  
  - **Không** cho user tự nhập số; `shippingFee` lấy từ kết quả API (hoặc mặc định nếu API lỗi).
- Sau khi **xác nhận đơn** (seller/auto): gọi API **tạo đơn vận chuyển**, lưu **mã vận đơn** vào Order (cần thêm trường `shippingOrderCode` hoặc tương đương).
- **Tracking**: trang chi tiết đơn hàng hiển thị link/trạng thái từ GHN/GHTK.

---

## Phương Án B: Tự Làm Hệ Thống Shipping (Quản Lý Shipper, Tự Tính Giá)

### 2.7 Ý tưởng

- Bạn **tự**:
  - Quản lý **shipper** (nhân viên/đối tác giao hàng): thông tin, khu vực phụ trách, trạng thái.
  - **Tính phí ship** theo quy tắc riêng: bảng giá theo vùng, theo km, theo kg, hoặc kết hợp.
  - **Phân đơn** cho shipper (gán đơn vào shipper theo khu vực/tải).
  - **Theo dõi** trạng thái giao (đã lấy, đang giao, đã giao, thất bại…).

### 2.8 Cần xây những gì

| Hạng mục | Nội dung |
|----------|----------|
| **Quản lý shipper** | CRUD shipper, khu vực hoạt động (quận/huyện, tỉnh), SĐT, trạng thái (rảnh/bận). Có thể cần app mobile hoặc web cho shipper nhận đơn, cập nhật trạng thái. |
| **Bảng giá / công thức** | Ví dụ: nội thành 20k, ngoại thành 30k, tỉnh xa 40k; hoặc theo km: 5k/km; hoặc theo kg: 3k/kg. Cần cấu hình linh hoạt (theo vùng, theo khoảng kg). |
| **Tính phí** | Service nhận: điểm đi, điểm đến, cân nặng (và có thể kích thước) → áp dụng bảng giá → trả về phí. Cần map địa chỉ → vùng (nội thành/ngoại thành/xa). |
| **Phân đơn** | Khi đơn chuyển trạng thái “sẵn sàng giao”: gán cho shipper (thủ công hoặc tự động theo khu vực). |
| **Theo dõi** | Shipper cập nhật: đã lấy hàng, đang giao, đã giao, lỗi (không liên lạc được, từ chối…). Hiển thị trên trang đơn hàng. |
| **Hạ tầng** | App/ghi nhận cho shipper, thông báo (email/SMS/push) khi có đơn mới, báo cáo đơn trễ, đơn lỗi. |

### 2.9 Ưu điểm

- **Chủ động** giá, chính sách (miễn phí ship theo đơn, theo vùng, theo khách hàng thân…).
- **Không phụ thuộc** API bên ngoài (trừ bản đồ nếu dùng tính km).
- Có thể tối ưu **tuyến, tải** nếu nhiều đơn cùng khu vực (ghép đơn cho 1 shipper).

### 2.10 Nhược điểm & ràng buộc

- **Chi phí xây dựng và vận hành rất lớn**:
  - Phần mềm: quản lý shipper, bảng giá, phân đơn, tracking, (và app shipper).
  - Con người: tuyển, đào tạo, giám sát shipper; xử lý khiếu nại, mất hàng, trễ.
- **Trách nhiệm pháp lý**: mất hàng, tai nạn, bảo hiểm — bạn là bên tổ chức giao hàng.
- **Khó mở rộng nhanh** ra nhiều tỉnh/thành nếu không có mạng lưới.
- **Thời gian** để có hệ thống ổn: nhiều tháng, và cần quy trình vận hành rõ ràng.

→ Phù hợp **sàn lớn, hoặc doanh nghiệp đã có sẵn đội giao hàng**, không phù hợp cho dự án e-commerce vừa/nhỏ hoặc đồ án môn học trong giai đoạn đầu.

---

## Phương Án C: Cách Đơn Giản — Không API, Không Hệ Thống Shipper Phức Tạp

### 2.11 Ý tưởng

- **Không** gọi API GHN/GHTK.
- **Không** xây quản lý shipper, phân đơn, app shipper.
- Chỉ **cải thiện cách tính/ghi nhận phí ship** trong phạm vi phần mềm hiện tại.

### 2.12 Các biến thể

**C1. Bảng giá theo vùng (zone-based)**

- Định nghĩa **vùng**: ví dụ  
  - Vùng 1: nội thành HCM/Hà Nội (một số quận).  
  - Vùng 2: ngoại thành, tỉnh lân cận.  
  - Vùng 3: tỉnh xa.
- Map: `district` + `province` (hoặc `ward`) → **mã vùng**.
- Bảng giá: `zone_id` → `fee` (VD: 20_000, 30_000, 40_000).
- **Tính phí**: địa chỉ giao hàng → vùng → tra bảng → `shippingFee`.
- **Cần**:  
  - Bảng map địa chỉ → vùng (có thể dùng file tỉnh/huyện/xã của Tổng cục Thống kê hoặc nguồn mở).  
  - Cấu hình bảng giá (Admin hoặc cấu hình cố định).

**C2. Phí cố định (flat rate)**

- Một mức: ví dụ 25_000 VNĐ cho mọi đơn.  
- Hoặc: **miễn phí ship** đơn từ X VNĐ.
- **Tính phí**: không cần địa chỉ chi tiết, chỉ cần subtotal (cho điều kiện miễn ship).

**C3. Seller tự nhập / ước tính (cải thiện nhẹ)**

- Giữ user nhập, nhưng:  
  - Gợi ý: “Thường 20.000–40.000 VNĐ cho nội thành” (text).  
  - Hoặc: **Seller** (khi xác nhận đơn) **sửa** `shippingFee` nếu cần, rồi mới chốt.  
- Vẫn không “tính” tự động, nhưng ít lệch hơn so với để khách tự bấm 0.

### 2.13 Ưu / nhược

- **Ưu**: triển khai nhanh, không phụ thuộc bên thứ 3, không cần đội shipper. Phù hợp **MVP, đồ án, giai đoạn chạy thử**.
- **Nhược**: không sát phí thực tế như GHN/GHTK; không có tracking, không tạo vận đơn. Giao hàng vẫn do Seller tự thuê xe, gửi GHN/GHTK thủ công, hoặc tự giao.

---

## 3. So Sánh Nhanh

| Tiêu chí | A: API GHN/GHTK/… | B: Tự làm hệ thống shipping | C: Bảng giá / flat / seller nhập |
|----------|-------------------|------------------------------|-----------------------------------|
| **Độ phức tạp** | Trung bình (tích hợp API, chuẩn địa chỉ, weight) | Rất cao (phần mềm + vận hành) | Thấp |
| **Thời gian** | Vài tuần | Nhiều tháng | Vài ngày – 1–2 tuần |
| **Chi phí vận hành** | Phí dịch vụ đối tác (đã nằm trong giá ship) | Rất cao (shipper, quản lý, bảo hiểm…) | Gần như không thêm |
| **Độ chính xác phí** | Cao (theo bảng giá đối tác) | Tùy bạn thiết kế | Trung bình / thấp (ước tính) |
| **Tạo vận đơn & tracking** | Có (nếu tích hợp tạo đơn) | Có (tự xây) | Không |
| **Quản lý shipper** | Không cần | Cần, rất nặng | Không cần |
| **Phù hợp** | Sàn/shop muốn phí chuẩn, tracking, mở rộng | Doanh nghiệp lớn, đã có đội giao | MVP, đồ án, thử nghiệm |

---

## 4. Gợi Ý Lộ Trình

### 4.1 Nếu ưu tiên **chạy nhanh, đủ dùng** (đồ án, demo, MVP)

→ **Bắt đầu bằng Phương án C**:

1. **C2 (flat hoặc miễn ship)**  
   - Ví dụ: 25_000 VNĐ mọi đơn; hoặc miễn ship đơn ≥ 500_000.  
   - Thay ô nhập tay bằng **một con số tính tự động** (hoặc ẩn ô, chỉ hiển thị “Phí ship: 25.000 VNĐ”).
2. **Hoặc C1 (bảng giá theo vùng)**  
   - Nếu đã có `shippingDistrict` + `shippingCity` (hoặc province): map đơn giản district → vùng → phí.  
   - Cần thêm bảng `shipping_zone`, `shipping_rate` (vùng, phí) và bảng map `district` → `zone` (có thể hard-code hoặc file).

→ **Không** cần weight, không cần chuẩn hóa ward. **Giao hàng thực tế**: Seller tự liên hệ GHN/GHTK/Viettel Post (hoặc xe ôm) bên ngoài, tự điền mã vận đơn vào đơn hàng (nếu sau này bạn thêm trường tracking).

### 4.2 Nếu muốn **phí sát thực tế và có tracking, tạo vận đơn** (hướng sản phẩm thật)

→ **Chọn Phương án A** (API bên thứ 3):

1. **Giai đoạn 1 – Tính phí (shipping fee)**  
   - Thêm **weight** cho Product/Variant.  
   - Chuẩn hóa **địa chỉ Shop** (ward/district/province hoặc mã) và **địa chỉ giao** (dropdown tỉnh → huyện → xã).  
   - Tích hợp **một** đơn vị (ví dụ **GHN**) API **calculate fee**.  
   - Checkout: gọi (qua backend) API tính phí → hiển thị phí, **không** cho nhập tay. Lưu `shippingFee` từ kết quả API.

2. **Giai đoạn 2 – Tạo đơn vận chuyển & tracking**  
   - Khi Seller (hoặc quy trình) xác nhận đơn: gọi API **tạo đơn vận chuyển** (GHN/GHTK).  
   - Lưu **mã vận đơn** vào Order.  
   - Trang chi tiết đơn: hiển thị trạng thái (và link tracking) từ API.

3. **Sau này**  
   - Có thể thêm GHTK, Viettel Post… để Seller chọn đơn vị hoặc để hệ thống so sánh giá.

→ **Không** nên xây **Phương án B** (tự quản lý shipper, tự tính giá phức tạp) trừ khi đây là sàn lớn hoặc bài toán đặc thù (ví dụ chỉ giao nội bộ một thành phố với đội xe riêng).

---

## 5. Trả Lời Trực Tiếp Câu Hỏi

- **“Call API bên thứ 3 hay tự làm hệ thống shipping riêng, quản lý shipper, tính giá?”**

  - **Tự làm hệ thống shipping riêng (quản lý shipper + tự tính giá)**:
    - **Không nên** làm trong giai đoạn đầu: chi phí và độ phức tạp rất lớn, phù hợp doanh nghiệp đã có sẵn đội giao hàng và nguồn lực vận hành.

  - **Call API bên thứ 3 (GHN, GHTK, Viettel Post, …)**:
    - **Nên** nếu mục tiêu là: phí ship **sát thực tế**, **tạo vận đơn**, **tracking**, và **không** muốn quản lý shipper.
    - Cần: thêm **weight** sản phẩm, **chuẩn hóa địa chỉ** (ward/district/province), tích hợp API tính phí và (sau đó) API tạo đơn.

  - **Tính toán giá ship “tự làm” nhưng đơn giản** (bảng giá theo vùng, hoặc flat, **không** quản lý shipper):
    - **Nên** nếu ưu tiên **triển khai nhanh**, MVP, đồ án: vài ngày đến vài tuần, không phụ thuộc bên ngoài. Giao hàng thực tế do Seller tự thuê GHN/GHTK/Viettel Post (hoặc xe ôm) bên ngoài.

---

## 6. Dữ Liệu Cần Bổ Sung (Cho Mọi Hướng Cải Thiện)

| Dữ liệu | Cần cho A (API) | Cần cho C1 (vùng) | Cần cho C2 (flat) |
|---------|-----------------|-------------------|-------------------|
| **Product/Variant: weight (kg)** | Có | Không (có thể dùng ước tính cố định) | Không |
| **Shop: ward, district, province (hoặc mã)** | Có (điểm lấy hàng) | Không (nếu chỉ tính theo địa chỉ giao) | Không |
| **Địa chỉ giao: ward, district, province** | Có (điểm giao), nên chuẩn mã | Có (để map vùng) | Không |
| **Bảng giá / bảng vùng** | Do API đối tác | Do bạn (cấu hình) | Không (1 con số hoặc rule đơn giản) |

---

## 7. Tóm Tắt Khuyến Nghị

1. **Không** xây hệ thống quản lý shipper và tự tính giá phức tạp (Phương án B) trong giai đoạn hiện tại.
2. **Nếu cần nhanh, đủ dùng**:  
   - Dùng **Phương án C**: flat phí hoặc bảng giá theo vùng; bỏ (hoặc ẩn) ô nhập tay `shippingFee`; tính tự động từ rule đơn giản.
3. **Nếu hướng tới sản phẩm thật, phí chuẩn, có tracking**:  
   - Dùng **Phương án A**: tích hợp **GHN** (hoặc **GHTK**) cho **tính phí** trước, sau đó **tạo đơn vận chuyển** và **tracking**.  
   - Cần: **weight** sản phẩm, **chuẩn hóa địa chỉ** Shop và địa chỉ giao (ward/district/province hoặc mã).

Bước kỹ thuật gần nhất cho **bất kỳ** hướng nào: **ngừng để khách tự nhập `shippingFee`** — thay bằng **một quy tắc** (flat, vùng, hoặc API) và **hiển thị phí đã tính** trước khi đặt hàng.
