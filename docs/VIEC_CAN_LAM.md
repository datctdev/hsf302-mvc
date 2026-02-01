# Việc cần làm – Payment & Settlement

**Dự án:** E-commerce (hsf302-mvc)  
**Cập nhật:** 01/02/2026  
**Phạm vi:** Refund, Multi-payment (ZaloPay/MoMo sandbox), Transaction + Wallet, Payout.

## 2. Refund (Hoàn tiền) – **Quốc**

**Mục tiêu:** Buyer/seller trả hàng → hoàn tiền; Order + Payment chuyển sang trạng thái REFUNDED.

### 2.1 Backend
- [ ] Thêm `PaymentStatus`: `REFUNDED`, (tuỳ chọn) `REFUNDING`.
- [ ] Service refund:
  - **VNPay:** Gọi API hoàn tiền VNPay (nếu có), verify response, cập nhật Payment + Order.
  - **COD:** Không gọi gateway; cập nhật Payment/Order REFUNDED, ghi chú hoàn tiền thủ công.
- [ ] Điều kiện: Chỉ refund khi Order đã thanh toán (Payment SUCCESS), chưa REFUNDED; quyền (admin/seller/buyer theo quy ước).
- [ ] Lưu raw response VNPay refund (audit).

### 2.2 API / MVC
- [ ] Route: ví dụ `POST /admin/orders/{id}/refund` hoặc `POST /seller/orders/{id}/refund` (tuỳ phân quyền).
- [ ] Validate: order thuộc shop (seller), trạng thái cho phép refund.

### 2.3 Frontend (Thymeleaf)
- [ ] Trang chi tiết đơn (admin/seller): nút "Hoàn tiền" khi đủ điều kiện.
- [ ] Form/confirm: lý do hoàn tiền (tuỳ chọn), xác nhận.

### 2.4 Tài liệu / Học
- [ ] Tham khảo luồng VNPay callback trong code (PaymentServiceImpl, VNPayServiceImpl).
- [ ] Tài liệu VNPay Refund API (sandbox/production).

**Đầu ra:** Refund VNPay + COD hoạt động; Order REFUNDED hiển thị đúng trên lịch sử đơn.

---

## 3. Multi-payment (ZaloPay, MoMo sandbox) – **Huy**

**Mục tiêu:** Thêm 2 cổng thanh toán sandbox: ZaloPay, MoMo; luồng tương tự VNPay (redirect → callback → verify).

### 3.1 Thiết kế chung
- [ ] Mở rộng `PaymentMethod`: thêm `ZALOPAY`, `MOMO`.
- [ ] Cấu hình: `application.properties` + `env.example` (appId, key, secret, returnUrl, …) cho từng gateway.
- [ ] Chuẩn hoá interface: ví dụ `PaymentGatewayService` (buildPayUrl, verifyCallback) hoặc giữ 2 service riêng ZaloPay/MoMo tương tự VNPayService.

### 3.2 ZaloPay (sandbox)
- [ ] Đăng ký sandbox ZaloPay, lấy thông tin app/key.
- [ ] Service: build redirect URL, tạo checksum/secure hash theo tài liệu ZaloPay.
- [ ] Callback: GET/POST (theo docs), verify, map status → Payment SUCCESS/FAILED, Order CONFIRMED/CANCELLED.
- [ ] Route: `/payments/zalopay/redirect`, `/payments/zalopay/callback`.
- [ ] Trang chọn thanh toán: thêm option ZaloPay.

### 3.3 MoMo (sandbox)
- [ ] Đăng ký sandbox MoMo, lấy partnerCode, accessKey, secretKey.
- [ ] Service: tạo request (signature theo MoMo), redirect sang MoMo.
- [ ] Callback (IPN / return): verify signature, cập nhật Payment + Order.
- [ ] Route: `/payments/momo/redirect`, `/payments/momo/callback` (hoặc IPN URL riêng).
- [ ] Trang chọn thanh toán: thêm option MoMo.

### 3.4 Chung
- [ ] Lưu raw callback (gateway_response) cho từng gateway.
- [ ] Bảo mật: verify checksum/signature; không hardcode secret.
- [ ] Cập nhật Payment.readme (hoặc doc riêng) mô tả ZaloPay/MoMo sandbox.

**Đầu ra:** Buyer chọn ZaloPay hoặc MoMo → redirect → thanh toán sandbox → callback → Order/Payment cập nhật đúng.

---

## 4. Transaction + Wallet – **Phát**

**Mục tiêu:** Ledger rõ ràng: Wallet (số dư), Transaction (ghi nợ/ghi có). Nền tảng cho escrow, balance seller/buyer, hoàn tiền vào ví, rút tiền.

### 4.1 Thiết kế domain
- [ ] **Wallet:** bảng `wallets` (user_id/account_id, type: BUYER/SELLER, balance, currency, updated_at). Hoặc 1 wallet/user, role xác định cách dùng.
- [ ] **Transaction:** bảng `transactions` (id, wallet_id, type: CREDIT/DEBIT, amount, balance_after, reference_type: ORDER/REFUND/PAYOUT, reference_id, order_id/payment_id/payout_id, description, created_at).
- [ ] Quy ước: Order thanh toán → debit buyer (hoặc ghi nhận từ Payment gateway), credit seller pending; Order DELIVERED → chuyển seller pending → available; Refund → debit seller (hoặc platform), credit buyer; Payout → debit seller wallet.

### 4.2 Core service
- [ ] `WalletService`: getOrCreateWallet(userId, type), getBalance(walletId).
- [ ] `TransactionService`: recordTransaction(walletId, type, amount, referenceType, referenceId, description). Đảm bảo cập nhật balance nguyên tử (DB lock hoặc optimistic).
- [ ] Tích hợp với Order/Payment: khi Payment SUCCESS → tạo transaction (buyer debit / seller credit pending hoặc available tùy quy tắc escrow). Khi Order DELIVERED → nếu dùng escrow: chuyển pending → available.
- [ ] Refund: tạo transaction (seller debit, buyer credit) hoặc (platform credit buyer); đồng bộ với Refund flow của Quốc.

### 4.3 Quy tắc kế toán
- [ ] Document: luồng Order paid → Transaction; Order delivered → Transaction; Refund → Transaction; Payout → Transaction.
- [ ] Đối soát: sum(Transaction) = Wallet.balance cho từng wallet.

**Đầu ra:** Wallet + Transaction chạy ổn định; mỗi Order paid/delivered/refund có bản ghi Transaction; balance đúng.

---

## 5. Payout (Seller rút tiền) – **Huy** (backend) + **Quốc** (UI & tích hợp)

**Mục tiêu:** Seller xem “số dư có thể rút”, tạo yêu cầu rút; Admin duyệt và đánh dấu đã chuyển khoản; nếu có Wallet thì payout debit wallet.

### 5.1 Backend (Huy)
- [ ] Bảng `payouts`: id, seller_id (account_id), amount, status (PENDING, APPROVED, PAID, REJECTED), bank_name, bank_account, bank_holder, note, paid_at, created_at, updated_at.
- [ ] Nếu **có Wallet:** số dư có thể rút = wallet balance (sau khi trừ reserved nếu có). Payout = tạo Transaction DEBIT seller wallet + tạo bản ghi Payout.
- [ ] Nếu **chưa có Wallet:** số dư có thể rút = SUM(subtotal - platform_commission) trên các Order DELIVERED chưa “settled”. Bảng `order_settlements` (order_id, payout_id) hoặc cột `order.settled_payout_id` để đánh dấu đơn đã thanh toán cho seller.
- [ ] Service: getAvailableBalance(sellerId), createPayoutRequest(sellerId, amount, bankInfo), listPayouts(sellerId), adminApprove/Reject, adminMarkPaid(payoutId).
- [ ] Validation: amount <= available balance; seller chỉ rút của mình; admin mới duyệt/paid.

### 5.2 API / MVC (Huy + Quốc)
- [ ] Seller: GET `/seller/payouts` (danh sách yêu cầu), GET `/seller/payouts/balance` (số dư), POST `/seller/payouts/request` (tạo yêu cầu).
- [ ] Admin: GET `/admin/payouts` (danh sách chờ duyệt), POST `/admin/payouts/{id}/approve`, POST `/admin/payouts/{id}/reject`, POST `/admin/payouts/{id}/mark-paid`.

### 5.3 Frontend (Quốc)
- [ ] Trang seller: “Số dư có thể rút” + bảng lịch sử payout + form “Yêu cầu rút” (số tiền, tên ngân hàng, số TK, chủ TK).
- [ ] Trang admin: danh sách payout PENDING/APPROVED, nút Duyệt/Từ chối/Đánh dấu đã chuyển.

### 5.4 Tích hợp (Quốc, có sự chỉ đạo của Huy)
- [ ] Khi Phát hoàn thành Wallet: nối Payout với Wallet (debit balance, tạo Transaction).
- [ ] Test: tạo order → delivered → kiểm tra balance → tạo payout → duyệt → mark paid.

**Đầu ra:** Seller rút được tiền qua luồng yêu cầu → duyệt → chuyển khoản (thủ công hoặc API sau); số dư và lịch sử hiển thị đúng.

---

## 6. Thứ tự thực hiện & phụ thuộc

```
1. Refund (Quốc)                    → Có thể làm song song, ít phụ thuộc
2. Multi-payment (Huy)              → Song song với Refund
3. Transaction + Wallet (Phát)     → Nên xong trước hoặc song song với Payout
4. Payout (Huy backend; Quốc UI)    → Backend có thể bắt đầu với “không Wallet”;
                                      khi có Wallet (Phát) thì tích hợp Payout ↔ Wallet
```

- **Refund** và **Multi-payment** không phụ thuộc nhau, có thể làm cùng lúc.
- **Payout** có thể làm “không Wallet” trước (tính số dư từ Order DELIVERED); khi **Transaction + Wallet** xong thì chuyển Payout sang debit Wallet.

---

## 7. Checklist theo người

### Quốc
- [ ] Đọc Payment.readme và luồng VNPay trong code.
- [ ] Refund: PaymentStatus + service + route + nút Hoàn tiền (admin/seller).
- [ ] Payout: UI seller (balance, form yêu cầu rút, danh sách) + UI admin (danh sách, duyệt, mark paid).
- [ ] Hỗ trợ tích hợp Payout với Wallet khi Phát bàn giao.

### Huy
- [ ] ZaloPay sandbox: config, redirect, callback, verify.
- [ ] MoMo sandbox: config, redirect, callback/IPN, verify.
- [ ] Payout: bảng payouts, service (balance, request, approve, mark paid), route backend.
- [ ] Hướng dẫn Quốc tích hợp UI Payout với API.

### Phát)
- [ ] Thiết kế Wallet + Transaction (bảng, quy tắc kế toán).
- [ ] WalletService, TransactionService, tích hợp Order/Payment/Refund.
- [ ] Document luồng Transaction cho Huy/Quốc tích hợp Payout.

---

## 8. Tài liệu tham khảo

- VNPay: callback, refund API (docs).
- ZaloPay: Sandbox, API redirect & callback.
- MoMo: Sandbox, API payment & IPN/return.

---

*Tài liệu này nên cập nhật khi hoàn thành từng mục hoặc đổi phân công.*
