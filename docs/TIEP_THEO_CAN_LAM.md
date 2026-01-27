### Ưu tiên thấp / dài hạn
8. **Wallet & Transaction**  
   - **Hiện trạng:** Chưa có entity Wallet/Transaction. Thanh toán hiện là Payment gắn Order (1-1), không có “ví” hay “dòng tiền” cho user/shop.  
   - **Khi nào cần:** Khi product yêu cầu: số dư ví, nạp/rút tiền, chi tiền khi mua/hoa hồng khi bán, đối soát.  
   - **Việc cần làm:** Thiết kế Wallet (user_id/shop_id, balance), Transaction (ref_type=ORDER/TOPUP/WITHDRAW/COMMISSION…), service ghi nhận biến động, và đảm bảo mọi thao tác tiền dùng `@Transactional`.

9. **Địa chỉ Shop đầy đủ (GHN)**  
   - **Hiện trạng:** Nhiều chỗ báo “shop chưa có địa chỉ đầy đủ (district_id, ward_code)”. ShippingService/GHN cần các mã này.  
   - **Việc cần làm:** Form sửa shop có thêm trường (hoặc dropdown) chọn Tỉnh/Thành, Quận/Huyện, Phường/Xã và lưu `district_id`, `ward_code` (có thể gọi API GHN lấy danh sách địa chỉ).  
   - **Tác động:** Giảm lỗi khi tạo đơn, tạo vận đơn GHN.

---