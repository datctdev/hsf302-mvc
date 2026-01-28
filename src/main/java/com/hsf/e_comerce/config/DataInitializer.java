package com.hsf.e_comerce.config;

import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.order.entity.Order;
import com.hsf.e_comerce.order.entity.OrderItem;
import com.hsf.e_comerce.order.repository.OrderItemRepository;
import com.hsf.e_comerce.order.repository.OrderRepository;
import com.hsf.e_comerce.order.valueobject.OrderStatus;
import com.hsf.e_comerce.platform.entity.PlatformSetting;
import com.hsf.e_comerce.platform.repository.PlatformSettingRepository;
import com.hsf.e_comerce.product.entity.Product;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.entity.ProductImage;
import com.hsf.e_comerce.product.entity.ProductVariant;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import com.hsf.e_comerce.product.repository.ProductImageRepository;
import com.hsf.e_comerce.product.repository.ProductRepository;
import com.hsf.e_comerce.product.repository.ProductVariantRepository;
import com.hsf.e_comerce.product.valueobject.ProductStatus;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shop.valueobject.ShopStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductCategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeDefaultUsers();
        initializeProductCategories();
        initializePlatformSettings();
        initializeSampleShopsProductsAndOrders();
    }

    private void initializePlatformSettings() {
        log.info("Starting platform settings initialization...");
        try {
            if (!platformSettingRepository.existsByKey(PlatformSetting.KEY_COMMISSION_RATE)) {
                PlatformSetting setting = new PlatformSetting();
                setting.setKey(PlatformSetting.KEY_COMMISSION_RATE);
                setting.setValue("10");
                platformSettingRepository.save(setting);
                log.info("✓ Created platform setting: commission_rate = 10%");
            } else {
                log.info("→ Platform setting commission_rate already exists.");
            }
        } catch (Exception e) {
            log.error("✗ Error initializing platform settings: {}", e.getMessage(), e);
        }
        log.info("Platform settings initialization completed.");
    }

    private void initializeRoles() {
        log.info("Starting roles initialization...");
        List<String> roleNames = Arrays.asList("ROLE_BUYER", "ROLE_SELLER", "ROLE_ADMIN");
        
        for (String roleName : roleNames) {
            try {
                if (!roleRepository.existsByName(roleName)) {
                    Role role = new Role();
                    role.setName(roleName);
                    Role savedRole = roleRepository.save(role);
                    log.info("✓ Created role: {} with ID: {}", roleName, savedRole.getId());
                } else {
                    Role existingRole = roleRepository.findByName(roleName).orElse(null);
                    log.info("→ Role already exists: {} (ID: {})", roleName, 
                            existingRole != null ? existingRole.getId() : "N/A");
                }
            } catch (Exception e) {
                log.error("✗ Error creating role {}: {}", roleName, e.getMessage(), e);
            }
        }
        log.info("Roles initialization completed.");
    }

    private void initializeDefaultUsers() {
        log.info("Starting default users initialization...");
        
        // Tạo tài khoản BUYER
        createDefaultUser("buyer@gmail.com", "buyer123@", "Người Mua", "ROLE_BUYER");
        
        // Tạo tài khoản SELLER
        User sellerUser = createDefaultUser("seller@gmail.com", "seller123@", "Người Bán", "ROLE_SELLER");
        if (sellerUser != null && "ROLE_SELLER".equals(sellerUser.getRole().getName())) {
            createDefaultShopForSeller(sellerUser);
        }
        
        // Tạo tài khoản ADMIN
        createDefaultUser("admin@gmail.com", "admin123@", "Quản Trị Viên", "ROLE_ADMIN");
        
        log.info("Default users initialization completed.");
    }

    private User createDefaultUser(String email, String password, String fullName, String roleName) {
        try {
            // Đảm bảo role đã được tạo trước
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName + ". Please ensure roles are initialized first."));
            
            if (!userRepository.existsByEmail(email)) {
                // Tạo user mới với role
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setFullName(fullName);
                user.setIsActive(true);
                user.setEmailVerified(true); // Tài khoản mặc định (test) coi như đã xác minh
                user.setRole(role); // Set role cho user
                
                user = userRepository.save(user);
                
                // Verify role was set correctly
                if (user.getRole() != null && user.getRole().getName().equals(roleName)) {
                    log.info("✓ Created user: {} ({}) with role {}", email, fullName, roleName);
                } else {
                    log.warn("⚠ User created but role verification failed: {} (expected: {})", 
                            user.getRole() != null ? user.getRole().getName() : "null", roleName);
                }
                return user;
            } else {
                // User đã tồn tại, kiểm tra và update role nếu cần
                User existingUser = userRepository.findByEmail(email)
                        .orElse(null);
                if (existingUser != null) {
                    boolean needSave = false;
                    if (existingUser.getRole() == null || !existingUser.getRole().getName().equals(roleName)) {
                        existingUser.setRole(role);
                        needSave = true;
                    }
                    if (!Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                        existingUser.setEmailVerified(true); // Tài khoản mặc định coi như đã xác minh
                        needSave = true;
                    }
                    if (needSave) {
                        existingUser = userRepository.save(existingUser);
                        log.info("✓ Updated existing user: {} with role {}", email, roleName);
                    } else {
                        log.info("→ User already exists with correct role: {} ({})", email, roleName);
                    }
                    // Nếu role là SELLER và chưa có shop, tạo shop
                    if ("ROLE_SELLER".equals(roleName) && !shopRepository.existsByUserId(existingUser.getId())) {
                        createDefaultShopForSeller(existingUser);
                    }
                    return existingUser;
                } else {
                    log.warn("→ User exists but could not be loaded: {}", email);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("✗ Error creating/updating user {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    private void createDefaultShopForSeller(User sellerUser) {
        try {
            // Kiểm tra shop đã tồn tại chưa
            if (shopRepository.existsByUserId(sellerUser.getId())) {
                log.info("→ Shop already exists for seller: {}", sellerUser.getEmail());
                return;
            }

            // Tạo shop mặc định cho seller
            Shop shop = new Shop();
            shop.setUser(sellerUser);
            
            // Tạo tên shop mặc định (tránh trùng)
            String defaultShopName = "Shop của " + (sellerUser.getFullName() != null ? sellerUser.getFullName() : sellerUser.getEmail());
            String shopName = defaultShopName;
            int suffix = 1;
            while (shopRepository.existsByName(shopName)) {
                shopName = defaultShopName + " " + suffix;
                suffix++;
            }
            shop.setName(shopName);
            
            shop.setDescription("Shop mặc định cho seller test");
            shop.setPhoneNumber(sellerUser.getPhoneNumber());
            shop.setStatus(ShopStatus.ACTIVE);
            
            shop = shopRepository.save(shop);
            log.info("✓ Created default shop: {} for seller: {}", shopName, sellerUser.getEmail());
        } catch (Exception e) {
            log.error("✗ Error creating shop for seller {}: {}", sellerUser.getEmail(), e.getMessage(), e);
        }
    }

    private void initializeProductCategories() {
        log.info("Starting product categories initialization...");
        
        // Định nghĩa cấu trúc categories chỉ dành cho mặt hàng điện tử (parent -> children)
        Map<String, List<String>> categoryStructure = new HashMap<>();
        categoryStructure.put("Điện Tử", Arrays.asList(
            "Điện Thoại", 
            "Laptop", 
            "Máy Tính Bảng", 
            "Tai Nghe", 
            "Loa",
            "Màn Hình",
            "Bàn Phím",
            "Chuột",
            "Webcam",
            "Ổ Cứng",
            "USB",
            "Thẻ Nhớ",
            "Sạc Dự Phòng",
            "Cáp Sạc",
            "Ốp Lưng",
            "Màn Hình Máy Tính",
            "Máy In",
            "Router",
            "Modem",
            "Smartwatch",
            "Máy Ảnh",
            "Máy Quay Phim",
            "Tivi",
            "Tủ Lạnh",
            "Máy Giặt",
            "Điều Hòa",
            "Máy Lọc Không Khí",
            "Robot Hút Bụi",
            "Lò Vi Sóng",
            "Bếp Từ",
            "Nồi Cơm Điện",
            "Máy Xay Sinh Tố",
            "Máy Pha Cà Phê",
            "Quạt Điện",
            "Đèn LED",
            "Ổ Cắm Thông Minh",
            "Công Tắc Thông Minh",
            "Camera An Ninh",
            "Khóa Cửa Thông Minh"
        ));
        
        // Tạo root categories trước
        Map<String, ProductCategory> rootCategories = new HashMap<>();
        for (String rootName : categoryStructure.keySet()) {
            try {
                if (!categoryRepository.findByName(rootName).isPresent()) {
                    ProductCategory category = new ProductCategory();
                    category.setName(rootName);
                    category.setParent(null);
                    category = categoryRepository.save(category);
                    rootCategories.put(rootName, category);
                    log.info("✓ Created root category: {}", rootName);
                } else {
                    ProductCategory existing = categoryRepository.findByName(rootName).orElse(null);
                    if (existing != null) {
                        rootCategories.put(rootName, existing);
                        log.info("→ Root category already exists: {}", rootName);
                    }
                }
            } catch (Exception e) {
                log.error("✗ Error creating root category {}: {}", rootName, e.getMessage(), e);
            }
        }
        
        // Tạo child categories
        for (Map.Entry<String, List<String>> entry : categoryStructure.entrySet()) {
            String parentName = entry.getKey();
            List<String> childrenNames = entry.getValue();
            ProductCategory parent = rootCategories.get(parentName);
            
            if (parent == null) {
                log.warn("⚠ Parent category not found: {}, skipping children", parentName);
                continue;
            }
            
            for (String childName : childrenNames) {
                try {
                    if (!categoryRepository.findByName(childName).isPresent()) {
                        ProductCategory child = new ProductCategory();
                        child.setName(childName);
                        child.setParent(parent);
                        categoryRepository.save(child);
                        log.info("✓ Created child category: {} -> {}", parentName, childName);
                    } else {
                        log.info("→ Child category already exists: {} -> {}", parentName, childName);
                    }
                } catch (Exception e) {
                    log.error("✗ Error creating child category {} -> {}: {}", parentName, childName, e.getMessage(), e);
                }
            }
        }
        
        log.info("Product categories initialization completed.");
    }

    /**
     * Tạo dữ liệu mẫu: 1 seller, 1 shop, 20 sản phẩm đồ điện tử, và một số đơn hàng.
     * Chỉ chạy khi chưa có đơn nào (tránh trùng khi restart).
     */
    private void initializeSampleShopsProductsAndOrders() {
        log.info("Starting sample shops/products/orders initialization...");
        try {
            if (orderRepository.count() > 0) {
                log.info("→ Đã có đơn hàng, bỏ qua tạo dữ liệu mẫu.");
                return;
            }

            User buyer = userRepository.findByEmail("buyer@gmail.com").orElse(null);
            User seller = userRepository.findByEmail("seller@gmail.com").orElse(null);
            if (buyer == null || seller == null) {
                log.warn("⚠ Buyer hoặc Seller chưa tồn tại, bỏ qua sample data.");
                return;
            }

            Shop shop = shopRepository.findByUserId(seller.getId()).orElse(null);
            if (shop == null) {
                log.warn("⚠ Shop của seller chưa tồn tại, bỏ qua sample data.");
                return;
            }

            // Cập nhật tên/ mô tả shop cho rõ là shop điện tử
            if (shop.getDescription() == null || shop.getDescription().contains("mặc định")) {
                shop.setName("TechZone – Đồ Điện Tử");
                shop.setDescription("Chuyên điện thoại, laptop, tai nghe, phụ kiện công nghệ chính hãng.");
                shopRepository.save(shop);
            }

            // 20 sản phẩm đồ điện tử + ảnh phù hợp (Unsplash, 400px)
            ProductCategory catPhone = categoryRepository.findByName("Điện Thoại").orElse(null);
            ProductCategory catLaptop = categoryRepository.findByName("Laptop").orElse(null);
            ProductCategory catTablet = categoryRepository.findByName("Máy Tính Bảng").orElse(null);
            ProductCategory catHeadphone = categoryRepository.findByName("Tai Nghe").orElse(null);
            ProductCategory catSpeaker = categoryRepository.findByName("Loa").orElse(null);
            ProductCategory catScreen = categoryRepository.findByName("Màn Hình").orElse(null);
            ProductCategory catKeyboard = categoryRepository.findByName("Bàn Phím").orElse(null);
            ProductCategory catMouse = categoryRepository.findByName("Chuột").orElse(null);
            ProductCategory catUSB = categoryRepository.findByName("USB").orElse(null);
            ProductCategory catPowerbank = categoryRepository.findByName("Sạc Dự Phòng").orElse(null);
            ProductCategory catWatch = categoryRepository.findByName("Smartwatch").orElse(null);
            ProductCategory catRouter = categoryRepository.findByName("Router").orElse(null);
            ProductCategory catTV = categoryRepository.findByName("Tivi").orElse(null);
            ProductCategory catCamera = categoryRepository.findByName("Máy Ảnh").orElse(null);
            ProductCategory catDefault = catPhone != null ? catPhone : categoryRepository.findAll().stream().findFirst().orElse(null);

            Product[] products = new Product[20];
            ProductVariant[] variants = new ProductVariant[20];

            // 1–5: Điện thoại, tablet, laptop
            products[0] = createProduct(shop, "Điện thoại Samsung Galaxy A54 5G", "Màn hình Super AMOLED 6.4\", chip Exynos 1380, camera 50MP.", "SEED-SKU-001", new BigDecimal("7990000"), catPhone, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400");
            variants[0] = createVariant(products[0], "Màu", "Đen", "SEED-V-001", 50);
            products[1] = createProduct(shop, "iPhone 15 128GB", "Chip A16 Bionic, camera chính 48MP, Dynamic Island.", "SEED-SKU-002", new BigDecimal("21990000"), catPhone, "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400");
            variants[1] = createVariant(products[1], "Màu", "Xanh Midnight", "SEED-V-002", 30);
            products[2] = createProduct(shop, "Laptop Dell XPS 15", "Intel Core i7, 16GB RAM, SSD 512GB, màn hình 15.6\" Full HD.", "SEED-SKU-003", new BigDecimal("35990000"), catLaptop, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400");
            variants[2] = createVariant(products[2], "Màu", "Bạc", "SEED-V-003", 20);
            products[3] = createProduct(shop, "Máy tính bảng iPad Air M2", "Chip M2, màn hình 10.9\", hỗ trợ Apple Pencil 2.", "SEED-SKU-004", new BigDecimal("14990000"), catTablet != null ? catTablet : catDefault, "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400");
            variants[3] = createVariant(products[3], "Dung lượng", "64GB", "SEED-V-004", 25);
            products[4] = createProduct(shop, "Laptop Asus Zenbook 14", "OLED 2.8K, Intel Core i5, 8GB RAM, nhẹ 1.2kg.", "SEED-SKU-005", new BigDecimal("22990000"), catLaptop, "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=400");
            variants[4] = createVariant(products[4], "Màu", "Indie Black", "SEED-V-005", 15);

            // 6–10: Tai nghe, loa, sạc, USB
            products[5] = createProduct(shop, "Tai nghe AirPods Pro 2", "Chống ồn chủ động, chip H2, MagSafe.", "SEED-SKU-006", new BigDecimal("5990000"), catHeadphone != null ? catHeadphone : catDefault, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400");
            variants[5] = createVariant(products[5], "Phiên bản", "USB-C", "SEED-V-006", 40);
            products[6] = createProduct(shop, "Loa Bluetooth JBL Flip 6", "Công suất 20W, chống nước IP67, pin 12 giờ.", "SEED-SKU-007", new BigDecimal("3290000"), catSpeaker != null ? catSpeaker : catDefault, "https://images.unsplash.com/photo-1545127398-14699f92334b?w=400");
            variants[6] = createVariant(products[6], "Màu", "Xanh Dương", "SEED-V-007", 35);
            products[7] = createProduct(shop, "Sạc dự phòng Anker 20000mAh", "Sạc nhanh PD 20W, 2 cổng USB-A, 1 USB-C.", "SEED-SKU-008", new BigDecimal("690000"), catPowerbank != null ? catPowerbank : catDefault, "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=400");
            variants[7] = createVariant(products[7], "Màu", "Đen", "SEED-V-008", 80);
            products[8] = createProduct(shop, "USB 3.2 SanDisk 64GB", "Tốc độ đọc 150MB/s, gọn nhẹ, bảo hành 5 năm.", "SEED-SKU-009", new BigDecimal("199000"), catUSB != null ? catUSB : catDefault, "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400");
            variants[8] = createVariant(products[8], "Dung lượng", "64GB", "SEED-V-009", 100);
            products[9] = createProduct(shop, "Tai nghe Sony WH-1000XM5", "Chống ồn hàng đầu, pin 30h, đa điểm kết nối.", "SEED-SKU-010", new BigDecimal("8990000"), catHeadphone != null ? catHeadphone : catDefault, "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=400");
            variants[9] = createVariant(products[9], "Màu", "Bạc", "SEED-V-010", 18);

            // 11–15: Màn hình, bàn phím, chuột, smartwatch, router
            products[10] = createProduct(shop, "Màn hình Dell S2721H 27 inch", "Full HD, IPS, 75Hz, viền mỏng.", "SEED-SKU-011", new BigDecimal("3990000"), catScreen != null ? catScreen : catDefault, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=400");
            variants[10] = createVariant(products[10], "Màu", "Đen", "SEED-V-011", 22);
            products[11] = createProduct(shop, "Bàn phím cơ Logitech G Pro", "Switch GX Blue, RGB, dây rút gọn.", "SEED-SKU-012", new BigDecimal("2490000"), catKeyboard != null ? catKeyboard : catDefault, "https://images.unsplash.com/photo-1541140530114-3cbebc939541?w=400");
            variants[11] = createVariant(products[11], "Layout", "US", "SEED-V-012", 30);
            products[12] = createProduct(shop, "Chuột không dây Logitech MX Master 3", "Ergonomic, cuộn siêu mượt, pin 70 ngày.", "SEED-SKU-013", new BigDecimal("2190000"), catMouse != null ? catMouse : catDefault, "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=400");
            variants[12] = createVariant(products[12], "Màu", "Xám Đen", "SEED-V-013", 45);
            products[13] = createProduct(shop, "Smartwatch Apple Watch Series 9", "GPS 41mm, màn hình Retina, đo SpO2, nhà thông minh.", "SEED-SKU-014", new BigDecimal("9990000"), catWatch != null ? catWatch : catDefault, "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400");
            variants[13] = createVariant(products[13], "Size", "41mm", "SEED-V-014", 20);
            products[14] = createProduct(shop, "Router WiFi 6 TP-Link Archer AX73", "Dual-band, băng tần 5GHz, phủ tốt cho căn hộ.", "SEED-SKU-015", new BigDecimal("1690000"), catRouter != null ? catRouter : catDefault, "https://images.unsplash.com/photo-1606904825846-647eb07f5be2?w=400");
            variants[14] = createVariant(products[14], "Màu", "Đen", "SEED-V-015", 28);

            // 16–20: Webcam, ổ cứng, thẻ nhớ, Tivi, máy ảnh
            products[15] = createProduct(shop, "Webcam Logitech C920 HD Pro", "Full HD 1080p 30fps, mic tích hợp, tương thích Zoom/Teams.", "SEED-SKU-016", new BigDecimal("1490000"), catDefault, "https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=400");
            variants[15] = createVariant(products[15], "Màu", "Đen", "SEED-V-016", 35);
            products[16] = createProduct(shop, "Ổ cứng SSD Samsung 980 1TB NVMe", "Tốc độ đọc 3500MB/s, M.2 PCIe 3.0, bảo hành 5 năm.", "SEED-SKU-017", new BigDecimal("1890000"), catDefault, "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400");
            variants[16] = createVariant(products[16], "Dung lượng", "1TB", "SEED-V-017", 40);
            products[17] = createProduct(shop, "Thẻ nhớ SanDisk Extreme 128GB", "U3 A2, tốc độ đọc 190MB/s, phù hợp quay 4K.", "SEED-SKU-018", new BigDecimal("449000"), catDefault, "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=400");
            variants[17] = createVariant(products[17], "Loại", "microSD", "SEED-V-018", 60);
            products[18] = createProduct(shop, "Tivi Samsung 55 inch Crystal 4K", "Crystal UHD, HDR, Tizen OS, 4K.", "SEED-SKU-019", new BigDecimal("12990000"), catTV != null ? catTV : catDefault, "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400");
            variants[18] = createVariant(products[18], "Kích thước", "55\"", "SEED-V-019", 12);
            products[19] = createProduct(shop, "Máy ảnh Sony Alpha A7 IV", "Full frame 33MP, 4K 60p, 5-axis IBIS, body.", "SEED-SKU-020", new BigDecimal("54990000"), catCamera != null ? catCamera : catDefault, "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=400");
            variants[19] = createVariant(products[19], "Body", "Chỉ body", "SEED-V-020", 8);

            // Một số đơn mẫu từ 1 seller (đa dạng trạng thái)
            createOrder(buyer, shop, "SEED-ORD-001", OrderStatus.DELIVERED, new BigDecimal("7990000"), new BigDecimal("35000"), 10.0, products[0], variants[0], 1, new BigDecimal("7990000"));
            createOrder(buyer, shop, "SEED-ORD-002", OrderStatus.DELIVERED, new BigDecimal("5990000"), new BigDecimal("30000"), 10.0, products[5], variants[5], 1, new BigDecimal("5990000"));
            createOrder(buyer, shop, "SEED-ORD-003", OrderStatus.CONFIRMED, new BigDecimal("690000"), new BigDecimal("22000"), 10.0, products[7], variants[7], 1, new BigDecimal("690000"));
            createOrder(buyer, shop, "SEED-ORD-004", OrderStatus.PROCESSING, new BigDecimal("3290000"), new BigDecimal("28000"), 10.0, products[6], variants[6], 1, new BigDecimal("3290000"));
            createOrder(buyer, shop, "SEED-ORD-005", OrderStatus.SHIPPING, new BigDecimal("199000"), new BigDecimal("15000"), 10.0, products[8], variants[8], 2, new BigDecimal("199000"));
            createOrder(buyer, shop, "SEED-ORD-006", OrderStatus.PENDING_PAYMENT, new BigDecimal("2490000"), new BigDecimal("25000"), 10.0, products[11], variants[11], 1, new BigDecimal("2490000"));
            createOrder(buyer, shop, "SEED-ORD-007", OrderStatus.CANCELLED, new BigDecimal("35990000"), new BigDecimal("50000"), 10.0, products[2], variants[2], 1, new BigDecimal("35990000"));

            log.info("✓ 1 seller, 1 shop, 20 sản phẩm điện tử và đơn mẫu đã tạo.");
        } catch (Exception e) {
            log.error("✗ Error creating sample data: {}", e.getMessage(), e);
        }
        log.info("Sample data initialization completed.");
    }

    private static final String SAMPLE_PRODUCT_IMAGE_URL =
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400";

    private Product createProduct(Shop shop, String name, String desc, String sku, BigDecimal basePrice, ProductCategory category) {
        return createProduct(shop, name, desc, sku, basePrice, category, null);
    }

    private Product createProduct(Shop shop, String name, String desc, String sku, BigDecimal basePrice, ProductCategory category, String imageUrl) {
        if (productRepository.existsBySku(sku)) {
            return productRepository.findBySku(sku).orElse(null);
        }
        Product p = new Product();
        p.setShop(shop);
        p.setName(name);
        p.setDescription(desc);
        p.setSku(sku);
        p.setStatus(ProductStatus.PUBLISHED);
        p.setBasePrice(basePrice);
        p.setWeight(500);
        p.setCategory(category != null ? category : null);
        p.setDeleted(false);
        p = productRepository.save(p);
        attachProductImage(p, imageUrl != null && !imageUrl.isBlank() ? imageUrl : SAMPLE_PRODUCT_IMAGE_URL);
        log.info("✓ Created sample product: {}", name);
        return p;
    }

    private void attachProductImage(Product product, String imageUrl) {
        if (product == null || imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        ProductImage img = new ProductImage();
        img.setProduct(product);
        img.setImageUrl(imageUrl);
        img.setIsThumbnail(true);
        img.setDisplayOrder(0);
        productImageRepository.save(img);
    }

    private ProductVariant createVariant(Product product, String name, String value, String sku, int stock) {
        if (productVariantRepository.existsBySku(sku)) {
            return productVariantRepository.findBySku(sku).orElse(null);
        }
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setName(name);
        v.setValue(value);
        v.setSku(sku);
        v.setStockQuantity(stock);
        v.setPriceModifier(BigDecimal.ZERO);
        v = productVariantRepository.save(v);
        return v;
    }

    /** Hoa hồng tính theo tiền hàng (theo sản phẩm): PlatformCommission = subtotal × rate%. VD: sản phẩm 100k → hoa hồng 10k (10%). */
    private void createOrder(User buyer, Shop shop, String orderNumber, OrderStatus status,
                            BigDecimal subtotal, BigDecimal shippingFee, double commissionRate,
                            Product product, ProductVariant variant, int qty, BigDecimal unitPrice) {
        if (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            return;
        }
        BigDecimal ship = shippingFee != null ? shippingFee : BigDecimal.ZERO;
        BigDecimal base = subtotal != null ? subtotal : BigDecimal.ZERO; // tiền hàng (tổng tiền sản phẩm)
        BigDecimal platformCommission = base.multiply(BigDecimal.valueOf(commissionRate / 100.0)).setScale(0, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(buyer);
        order.setShop(shop);
        order.setStatus(status);
        order.setShippingName("Nguyễn Văn Mua");
        order.setShippingPhone("0901234567");
        order.setShippingAddress("123 Đường Mẫu, Quận 1, TP.HCM");
        order.setShippingCity("TP. Hồ Chí Minh");
        order.setSubtotal(subtotal);
        order.setShippingFee(ship);
        order.setPlatformCommission(platformCommission);
        order.setCommissionRate(commissionRate);
        order.calculateTotal();
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setVariant(variant);
        item.setProductName(product.getName());
        item.setVariantName(variant.getName());
        item.setVariantValue(variant.getValue());
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.calculateTotalPrice();
        orderItemRepository.save(item);
        log.info("✓ Created sample order: {} status={}", orderNumber, status);
    }
}
