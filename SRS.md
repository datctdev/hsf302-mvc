# Đặc Tả Yêu Cầu Phần Mềm (SRS) - Sàn Thương Mại Điện Tử

**Phiên bản:** 1.0
**Ngày:** 08-01-2026

---

## 1. Giới thiệu (Introduction)

### 1.1. Mục đích (Purpose)
Tài liệu này định nghĩa các yêu cầu chức năng và phi chức năng cho dự án "Sàn Thương Mại Điện Tử". Mục đích của tài liệu là cung cấp một nguồn thông tin duy nhất, rõ ràng cho đội ngũ phát triển, kiểm thử, và các bên liên quan để hiểu rõ và triển khai sản phẩm đúng theo yêu cầu.

### 1.2. Phạm vi dự án (Scope)
Sản phẩm là một nền tảng Sàn thương mại điện tử (Multi-Vendor Marketplace) được xây dựng theo kiến trúc MVC truyền thống. Nền tảng cho phép các cá nhân, cửa hàng ("Người bán") đăng ký mở gian hàng và bán sản phẩm của mình. Người dùng thông thường ("Người mua") có thể tìm kiếm, xem, mua sản phẩm và thanh toán qua các cổng thanh toán trực tuyến. Quản trị viên ("Admin") có quyền quản lý toàn bộ hoạt động của sàn.

Dự án bao gồm các chức năng chính: quản lý người dùng và phân quyền, quản lý sản phẩm, giỏ hàng, đặt hàng, tích hợp thanh toán online, hệ thống đánh giá, và các tính năng hỗ trợ bởi AI (phân loại sản phẩm và chatbot).

### 1.3. Tổng quan (Overview)
Tài liệu này bao gồm:
- **Mô tả tổng quan:** Bối cảnh, chức năng chính và đối tượng người dùng của sản phẩm.
- **Yêu cầu chức năng:** Chi tiết các tính năng mà hệ thống phải cung cấp, được phân chia theo từng module.
- **Yêu cầu phi chức năng:** Các tiêu chí về hiệu năng, bảo mật, độ tin cậy của hệ thống.
- **Yêu cầu giao diện:** Mô tả các giao diện tương tác giữa hệ thống với người dùng và với các dịch vụ bên ngoài.

### 1.4. Định nghĩa, từ viết tắt
- **MVC:** Model-View-Controller
- **SRS:** Software Requirement Specification
- **Người mua (Buyer):** Người dùng có tài khoản để mua sắm.
- **Người bán (Seller):** Người dùng được cấp quyền để đăng bán sản phẩm.
- **Admin:** Quản trị viên hệ thống.
- **Sàn:** Nền tảng Sàn thương mại điện tử.

---

## 2. Mô tả tổng quan (Overall Description)

### 2.1. Bối cảnh sản phẩm (Product Perspective)
Sản phẩm là một hệ thống độc lập, được xây dựng từ đầu bằng Spring Boot (Java) cho backend và Thymeleaf cho frontend (server-side rendering). Hệ thống sẽ tích hợp với các dịch vụ bên ngoài bao gồm:
- Cổng thanh toán (VNPay, Momo) để xử lý giao dịch.
- Dịch vụ AI (xây dựng riêng bằng Python) để phân loại sản phẩm.
- Nền tảng Chatbot (ví dụ: Google Dialogflow) để hỗ trợ người dùng.

### 2.2. Chức năng sản phẩm (Product Functions)
- **F-AUTH:** Xác thực và quản lý người dùng (đăng ký, đăng nhập, phân quyền).
- **F-SHOP:** Quản lý gian hàng (đăng ký, duyệt, quản lý thông tin shop).
- **F-PRODUCT:** Quản lý sản phẩm (thêm, sửa, xóa, tìm kiếm, xem chi tiết).
- **F-CART:** Quản lý giỏ hàng.
- **F-ORDER:** Quản lý đơn hàng (tạo, xử lý, theo dõi).
- **F-PAYMENT:** Tích hợp thanh toán trực tuyến.
- **F-REVIEW:** Hệ thống đánh giá sản phẩm và người bán.
- **F-AI:** Các chức năng tích hợp AI.

### 2.3. Đối tượng người dùng (User Characteristics)
- **Người mua:** Có kiến thức cơ bản về sử dụng web để mua sắm online.
- **Người bán:** Có nhu cầu bán hàng online, có khả năng sử dụng các công cụ quản lý sản phẩm, đơn hàng.
- **Quản trị viên:** Có kiến thức kỹ thuật để quản lý hệ thống, duyệt yêu cầu, giải quyết tranh chấp.

### 2.4. Ràng buộc chung (General Constraints)
- Hệ thống phải được xây dựng trên nền tảng Java 17+, Spring Boot 3+.
- Giao diện người dùng phải được render phía server bằng Thymeleaf.
- Ngôn ngữ chính của hệ thống là tiếng Việt.
- Phải tuân thủ các quy định của pháp luật Việt Nam về thương mại điện tử và giao dịch trực tuyến.

---

## 3. Yêu cầu chức năng (Functional Requirements)

Phần này sẽ chi tiết hóa các chức năng của hệ thống.

### 3.1. Module Quản lý Người dùng và Xác thực (F-AUTH)

#### 3.1.1. Đăng ký tài khoản
- **ID:** `FUNC-AUTH-001`
- **Mô tả:** Người dùng vãng lai có thể đăng ký một tài khoản mới để trở thành "Người mua".
- **Chi tiết:**
    - Hệ thống phải cung cấp một trang đăng ký với các trường thông tin: Họ và tên, Email, Mật khẩu, Nhập lại mật khẩu.
    - Email phải là duy nhất trong toàn hệ thống. Hệ thống phải kiểm tra và báo lỗi nếu email đã tồn tại.
    - Mật khẩu phải có độ dài tối thiểu 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường và một chữ số.
    - Mật khẩu phải được mã hóa (hashed) trước khi lưu vào cơ sở dữ liệu.
    - Sau khi đăng ký thành công, hệ thống tự động gán vai trò `ROLE_BUYER` cho tài khoản và chuyển hướng người dùng đến trang đăng nhập.

#### 3.1.2. Đăng nhập
- **ID:** `FUNC-AUTH-002`
- **Mô tả:** Người dùng đã có tài khoản có thể đăng nhập vào hệ thống.
- **Chi tiết:**
    - Hệ thống phải cung cấp trang đăng nhập với các trường: Email, Mật khẩu.
    - Hệ thống phải xác thực thông tin đăng nhập. Nếu sai, hiển thị thông báo lỗi ngay trên trang đăng nhập.
    - Nếu đăng nhập thành công, hệ thống phải tạo một phiên làm việc (session) cho người dùng và chuyển hướng đến trang chủ.
    - Hệ thống phải có chức năng "Ghi nhớ đăng nhập" (Remember Me).

#### 3.1.3. Đăng xuất
- **ID:** `FUNC-AUTH-003`
- **Mô tả:** Người dùng đã đăng nhập có thể đăng xuất khỏi hệ thống.
- **Chi tiết:**
    - Khi người dùng nhấn nút "Đăng xuất", hệ thống phải hủy phiên làm việc hiện tại và chuyển hướng về trang chủ.

#### 3.1.4. Phân quyền
- **ID:** `FUNC-AUTH-004`
- **Mô tả:** Hệ thống phải hỗ trợ 3 vai trò người dùng: `ROLE_BUYER`, `ROLE_SELLER`, `ROLE_ADMIN`.
- **Chi tiết:**
    - `ROLE_BUYER`: Có quyền thực hiện các chức năng của người mua (xem sản phẩm, thêm vào giỏ hàng, đặt hàng, đánh giá). Đây là vai trò mặc định khi đăng ký.
    - `ROLE_SELLER`: Có tất cả quyền của `ROLE_BUYER`, cộng thêm quyền quản lý gian hàng của mình (quản lý sản phẩm, xử lý đơn hàng của shop).
    - `ROLE_ADMIN`: Có toàn quyền trên hệ thống, bao gồm quản lý người dùng, duyệt yêu cầu bán hàng, quản lý tất cả sản phẩm và đơn hàng.

*(Các module chức năng khác như Quản lý Shop, Sản phẩm, Giỏ hàng... sẽ được chi tiết hóa ở các phiên bản sau của tài liệu.)*

---

## 4. Yêu cầu phi chức năng (Non-functional Requirements)

### 4.1. Yêu cầu về hiệu năng (Performance)
- **ID:** `NON-FUNC-PERF-001`
- **Mô tả:** Thời gian phản hồi của trang không được vượt quá 3 giây cho 95% các yêu cầu trong điều kiện tải bình thường.
- **ID:** `NON-FUNC-PERF-002`
- **Mô tả:** Hệ thống phải có khả năng chịu tải 500 người dùng hoạt động đồng thời mà không suy giảm hiệu năng đáng kể.

### 4.2. Yêu cầu về bảo mật (Security)
- **ID:** `NON-FUNC-SEC-001`
- **Mô tả:** Mật khẩu người dùng phải được hash bằng thuật toán an toàn (ví dụ: bcrypt).
- **ID:** `NON-FUNC-SEC-002`
- **Mô tả:** Hệ thống phải được bảo vệ khỏi các lỗ hổng web phổ biến theo danh sách OWASP Top 10 (ví dụ: SQL Injection, Cross-Site Scripting - XSS).
- **ID:** `NON-FUNC-SEC-003`
- **Mô tả:** Các trang quản trị và trang nhạy cảm phải yêu cầu xác thực và phân quyền hợp lệ.

*(Tài liệu sẽ được cập nhật và chi tiết hóa trong quá trình phát triển dự án.)*