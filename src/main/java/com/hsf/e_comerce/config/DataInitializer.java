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
import com.hsf.e_comerce.platform.repository.CommissionRepository;
import com.hsf.e_comerce.platform.repository.PlatformSettingRepository;
import com.hsf.e_comerce.platform.service.CommissionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final CommissionService commissionService;
    private final CommissionRepository commissionRepository;

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
                user.setDeleted(false);
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

    /** Trả về shop của seller; tạo mới nếu chưa có, hoặc cập nhật tên/mô tả cho demo. */
    private Shop ensureShopForSeller(User seller, String shopName, String description) {
        Optional<Shop> existing = shopRepository.findByUserId(seller.getId());
        if (existing.isPresent()) {
            Shop s = existing.get();
            s.setName(shopName);
            s.setDescription(description);
            return shopRepository.save(s);
        }
        Shop shop = new Shop();
        shop.setUser(seller);
        String name = shopName;
        int suffix = 1;
        while (shopRepository.existsByName(name)) {
            name = shopName + " " + suffix;
            suffix++;
        }
        shop.setName(name);
        shop.setDescription(description);
        shop.setStatus(ShopStatus.ACTIVE);
        return shopRepository.save(shop);
    }

    /** Danh sách buyer cho demo (gồm buyer mặc định + 4 buyer thêm). */
    private List<User> ensureDemoBuyers(User defaultBuyer) {
        List<User> list = new ArrayList<>();
        list.add(defaultBuyer);
        String[][] extras = {
                {"buyer2@demo.com", "Trần Minh Tuấn"},
                {"buyer3@demo.com", "Lê Thị Hương"},
                {"buyer4@demo.com", "Phạm Đức Anh"},
                {"buyer5@demo.com", "Hoàng Thu Trang"}
        };
        for (String[] row : extras) {
            User u = createDefaultUser(row[0], "buyer123@", row[1], "ROLE_BUYER");
            if (u != null) list.add(u);
        }
        return list;
    }

    /** Tạo 6 sản phẩm + variant cho shop (demo). Thứ tự: name1, desc1, sku1, price1, ... x6. */
    private Product[] createProductsForShop(Shop shop, ProductCategory cat1, ProductCategory cat2, ProductCategory catDefault,
                                            String name1, String desc1, String sku1, String price1,
                                            String name2, String desc2, String sku2, String price2,
                                            String name3, String desc3, String sku3, String price3,
                                            String name4, String desc4, String sku4, String price4,
                                            String name5, String desc5, String sku5, String price5,
                                            String name6, String desc6, String sku6, String price6) {
        ProductCategory[] cats = new ProductCategory[]{cat1, cat2, catDefault};
        String[][] data = {
                {name1, desc1, sku1, price1}, {name2, desc2, sku2, price2}, {name3, desc3, sku3, price3},
                {name4, desc4, sku4, price4}, {name5, desc5, sku5, price5}, {name6, desc6, sku6, price6}
        };
        Product[] out = new Product[6];
        for (int i = 0; i < 6; i++) {
            ProductCategory cat = cats[i % cats.length];
            out[i] = createProduct(shop, data[i][0], data[i][1], data[i][2], new BigDecimal(data[i][3]), cat, SAMPLE_PRODUCT_IMAGE_URL);
            createVariant(out[i], "Màu", "Đen", data[i][2] + "-V", 20 + i * 5);
        }
        return out;
    }

    private Product[] mergeProductArrays(Product[] a, Product[] b) {
        Product[] out = new Product[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
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

            Product[] products = new Product[40];
            ProductVariant[] variants = new ProductVariant[40];

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

            // 21–40: Gấp đôi sản phẩm shop 1 (demo phong phú)
            products[20] = createProduct(shop, "Điện thoại Google Pixel 8", "Tensor G3, camera 50MP, Android 14.", "SEED-SKU-021", new BigDecimal("18990000"), catPhone, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400");
            variants[20] = createVariant(products[20], "Màu", "Obsidian", "SEED-V-021", 25);
            products[21] = createProduct(shop, "Laptop MacBook Air M3", "Chip M3, 8GB, SSD 256GB, 13.6\" Liquid Retina.", "SEED-SKU-022", new BigDecimal("27990000"), catLaptop, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400");
            variants[21] = createVariant(products[21], "Màu", "Midnight", "SEED-V-022", 12);
            products[22] = createProduct(shop, "Tai nghe Bose QuietComfort Ultra", "Chống ồn, Immersive Audio, pin 24h.", "SEED-SKU-023", new BigDecimal("8990000"), catHeadphone != null ? catHeadphone : catDefault, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400");
            variants[22] = createVariant(products[22], "Màu", "Smoke White", "SEED-V-023", 20);
            products[23] = createProduct(shop, "Loa Sonos Era 100", "Stereo, WiFi, AirPlay 2, giọng nói.", "SEED-SKU-024", new BigDecimal("5990000"), catSpeaker != null ? catSpeaker : catDefault, "https://images.unsplash.com/photo-1545127398-14699f92334b?w=400");
            variants[23] = createVariant(products[23], "Màu", "Đen", "SEED-V-024", 18);
            products[24] = createProduct(shop, "Màn hình LG UltraGear 27GP850", "2K 165Hz Nano IPS, 1ms, G-Sync.", "SEED-SKU-025", new BigDecimal("7490000"), catScreen != null ? catScreen : catDefault, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=400");
            variants[24] = createVariant(products[24], "Màu", "Đen", "SEED-V-025", 14);
            products[25] = createProduct(shop, "Bàn phím Keychron K2", "Wireless, Mac/Win, hot-swap.", "SEED-SKU-026", new BigDecimal("1290000"), catKeyboard != null ? catKeyboard : catDefault, "https://images.unsplash.com/photo-1541140530114-3cbebc939541?w=400");
            variants[25] = createVariant(products[25], "Switch", "Red", "SEED-V-026", 35);
            products[26] = createProduct(shop, "Chuột Razer DeathAdder V3", "30K DPI, 90g, 90h pin.", "SEED-SKU-027", new BigDecimal("1990000"), catMouse != null ? catMouse : catDefault, "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=400");
            variants[26] = createVariant(products[26], "Màu", "Đen", "SEED-V-027", 40);
            products[27] = createProduct(shop, "Smartwatch Samsung Galaxy Watch 6", "Wear OS, đo nhịp tim, pin 40h.", "SEED-SKU-028", new BigDecimal("6990000"), catWatch != null ? catWatch : catDefault, "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?w=400");
            variants[27] = createVariant(products[27], "Size", "44mm", "SEED-V-028", 22);
            products[28] = createProduct(shop, "Router Asus RT-AX86U", "WiFi 6, gaming, 2.5G port.", "SEED-SKU-029", new BigDecimal("3990000"), catRouter != null ? catRouter : catDefault, "https://images.unsplash.com/photo-1606904825846-647eb07f5be2?w=400");
            variants[28] = createVariant(products[28], "Màu", "Đen", "SEED-V-029", 16);
            products[29] = createProduct(shop, "iPad 10th gen", "A14, 10.9\", USB-C, nhiều màu.", "SEED-SKU-030", new BigDecimal("10990000"), catTablet != null ? catTablet : catDefault, "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400");
            variants[29] = createVariant(products[29], "Dung lượng", "64GB", "SEED-V-030", 30);
            products[30] = createProduct(shop, "Cáp USB-C to Lightning 2m", "MFi, sạc nhanh iPhone.", "SEED-SKU-031", new BigDecimal("490000"), catDefault, "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=400");
            variants[30] = createVariant(products[30], "Màu", "Trắng", "SEED-V-031", 80);
            products[31] = createProduct(shop, "Ổ cứng HDD Seagate 2TB", "2.5\" USB 3.0, backup.", "SEED-SKU-032", new BigDecimal("1290000"), catDefault, "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400");
            variants[31] = createVariant(products[31], "Màu", "Đen", "SEED-V-032", 45);
            products[32] = createProduct(shop, "Microphone Blue Yeti", "USB condenser, 4 chế độ thu.", "SEED-SKU-033", new BigDecimal("2490000"), catDefault, "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=400");
            variants[32] = createVariant(products[32], "Màu", "Đen", "SEED-V-033", 28);
            products[33] = createProduct(shop, "Đèn bàn LED Baseus", "Điều chỉnh độ sáng, sạc không dây.", "SEED-SKU-034", new BigDecimal("349000"), catDefault, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=400");
            variants[33] = createVariant(products[33], "Màu", "Trắng", "SEED-V-034", 60);
            products[34] = createProduct(shop, "Tivi TCL 50 inch 4K", "Android TV, Dolby Vision.", "SEED-SKU-035", new BigDecimal("8990000"), catTV != null ? catTV : catDefault, "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400");
            variants[34] = createVariant(products[34], "Kích thước", "50\"", "SEED-V-035", 15);
            products[35] = createProduct(shop, "Máy đọc sách Kindle Paperwhite", "6.8\", chống nước, 10 tuần pin.", "SEED-SKU-036", new BigDecimal("3990000"), catDefault, "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400");
            variants[35] = createVariant(products[35], "Phiên bản", "8GB", "SEED-V-036", 35);
            products[36] = createProduct(shop, "Ring Light 18 inch", "LED, 3 chế độ ánh sáng, chân đế.", "SEED-SKU-037", new BigDecimal("599000"), catDefault, "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=400");
            variants[36] = createVariant(products[36], "Màu", "Trắng", "SEED-V-037", 42);
            products[37] = createProduct(shop, "Hub USB-C 7-in-1", "HDMI 4K, SD, PD 100W.", "SEED-SKU-038", new BigDecimal("890000"), catDefault, "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400");
            variants[37] = createVariant(products[37], "Màu", "Bạc", "SEED-V-038", 55);
            products[38] = createProduct(shop, "Balo laptop 15.6 inch", "Chống nước, nhiều ngăn.", "SEED-SKU-039", new BigDecimal("449000"), catDefault, "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400");
            variants[38] = createVariant(products[38], "Màu", "Đen", "SEED-V-039", 70);
            products[39] = createProduct(shop, "Giá đỡ điện thoại ô tô", "Kẹp điều hòa, xoay 360.", "SEED-SKU-040", new BigDecimal("199000"), catDefault, "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=400");
            variants[39] = createVariant(products[39], "Màu", "Đen", "SEED-V-040", 90);

            // --- Demo: thêm buyer và seller/shop để dashboard phong phú ---
            List<User> demoBuyers = ensureDemoBuyers(buyer);
            List<Shop> allShops = new ArrayList<>();
            allShops.add(shop);
            List<Product[]> shopProducts = new ArrayList<>();
            shopProducts.add(products);

            User seller2 = createDefaultUser("seller2@demo.com", "seller123@", "Trần Văn Điện", "ROLE_SELLER");
            if (seller2 != null) {
                Shop shop2 = ensureShopForSeller(seller2, "Điện Máy Xanh", "Điện thoại, tivi, tủ lạnh chính hãng.");
                if (shop2 != null) {
                    allShops.add(shop2);
                    Product[] p2a = createProductsForShop(shop2, catPhone, catTV, catDefault,
                            "OPPO Reno 11", "Màn hình AMOLED 6.7\", camera 50MP.", "SEED-S2-001", "3990000",
                            "Tivi LG 43 inch 4K", "Smart TV webOS, HDR.", "SEED-S2-002", "8990000",
                            "Tủ lạnh Samsung 234 lít", "Inverter, tiết kiệm điện.", "SEED-S2-003", "7990000",
                            "Điện thoại Xiaomi Redmi Note 13", "Pin 5000mAh, sạc 33W.", "SEED-S2-004", "4990000",
                            "Loa JBL Charge 5", "Bluetooth, chống nước.", "SEED-S2-005", "3490000",
                            "Tai nghe JBL Tune 520BT", "Bluetooth 5.3, pin 40h.", "SEED-S2-006", "699000");
                    Product[] p2b = createProductsForShop(shop2, catPhone, catTV, catDefault,
                            "Nokia G42 5G", "Pin 5000mAh, màn hình 6.56\".", "SEED-S2-007", "3490000",
                            "Tivi Sony 43 inch Bravia", "4K HDR, Android TV.", "SEED-S2-008", "11990000",
                            "Máy giặt LG 9kg", "Inverter, cửa trước.", "SEED-S2-009", "8990000",
                            "Realme C55", "SuperVOOC 33W, 64MP.", "SEED-S2-010", "3990000",
                            "Loa Marshall Emberton", "Bluetooth, thiết kế iconic.", "SEED-S2-011", "2990000",
                            "Tai nghe SoundPEATS TrueFree", "Bluetooth 5.2, pin 17h.", "SEED-S2-012", "449000");
                    shopProducts.add(mergeProductArrays(p2a, p2b));
                }
            }
            User seller3 = createDefaultUser("seller3@demo.com", "seller123@", "Lê Thị Phụ Kiện", "ROLE_SELLER");
            if (seller3 != null) {
                Shop shop3 = ensureShopForSeller(seller3, "Phụ Kiện Pro", "Ốp lưng, cáp, sạc, bao da.");
                if (shop3 != null) {
                    allShops.add(shop3);
                    Product[] p3a = createProductsForShop(shop3, catDefault, catPowerbank, catUSB,
                            "Ốp lưng iPhone 15 silicone", "Chính hãng Apple.", "SEED-S3-001", "590000",
                            "Cáp sạc nhanh 20W USB-C", "PD 3.0, dây 2m.", "SEED-S3-002", "199000",
                            "Sạc dự phòng 10000mAh", "Sạc nhanh 18W.", "SEED-S3-003", "349000",
                            "Bao da iPad Air", "Bảo vệ 360 độ.", "SEED-S3-004", "299000",
                            "Giá đỡ laptop nhôm", "Góc nâng thoáng mát.", "SEED-S3-005", "189000",
                            "Webcam 1080p", "Full HD, mic tích hợp.", "SEED-S3-006", "449000");
                    Product[] p3b = createProductsForShop(shop3, catDefault, catPowerbank, catUSB,
                            "Ốp lưng Samsung Galaxy", "Trong suốt, chống xước.", "SEED-S3-007", "199000",
                            "Cáp Lightning 1m", "MFi, sạc sync.", "SEED-S3-008", "299000",
                            "Sạc dự phòng 20000mAh 2 cổng", "PD 22.5W.", "SEED-S3-009", "549000",
                            "Túi đựng MacBook", "Chống sốc, nhiều ngăn.", "SEED-S3-010", "399000",
                            "Bàn phím Bluetooth số", "Gọn, pin 6 tháng.", "SEED-S3-011", "249000",
                            "Miếng dán màn hình cường lực", "Full coverage 9H.", "SEED-S3-012", "149000");
                    shopProducts.add(mergeProductArrays(p3a, p3b));
                }
            }
            User seller4 = createDefaultUser("seller4@demo.com", "seller123@", "Nguyễn Laptop", "ROLE_SELLER");
            if (seller4 != null) {
                Shop shop4 = ensureShopForSeller(seller4, "Laptop Store", "Laptop gaming, văn phòng giá tốt.");
                if (shop4 != null) {
                    allShops.add(shop4);
                    Product[] p4a = createProductsForShop(shop4, catLaptop, catScreen, catDefault,
                            "Laptop HP Pavilion 15", "Ryzen 5, 8GB RAM, SSD 256GB.", "SEED-S4-001", "12990000",
                            "Laptop Lenovo IdeaPad 3", "Intel i3, 8GB, phù hợp học tập.", "SEED-S4-002", "9990000",
                            "Laptop Acer Aspire 5", "Ryzen 3, 8GB, 15.6\" FHD.", "SEED-S4-003", "10990000",
                            "Màn hình AOC 24 inch", "IPS, 75Hz, viền mỏng.", "SEED-S4-004", "2990000",
                            "Laptop MSI Gaming GF63", "i5, RTX 3050, 16GB.", "SEED-S4-005", "22990000",
                            "Bàn phím Dareu EK815", "Cơ RGB, giá rẻ.", "SEED-S4-006", "699000");
                    Product[] p4b = createProductsForShop(shop4, catLaptop, catScreen, catDefault,
                            "Laptop Dell Inspiron 15", "Intel i5, 8GB, SSD 512GB.", "SEED-S4-007", "14990000",
                            "Laptop Asus Vivobook 15", "Ryzen 5, 8GB, OLED.", "SEED-S4-008", "13990000",
                            "Laptop Gaming Acer Nitro 5", "i5, RTX 4050, 16GB.", "SEED-S4-009", "25990000",
                            "Màn hình BenQ 27 inch", "2K, 75Hz, Eye-Care.", "SEED-S4-010", "4990000",
                            "Laptop LG Gram 17", "Siêu nhẹ 1.35kg, pin lâu.", "SEED-S4-011", "39990000",
                            "Chuột gaming Logitech G102", "8000 DPI, RGB.", "SEED-S4-012", "449000");
                    shopProducts.add(mergeProductArrays(p4a, p4b));
                }
            }

            // Đơn mẫu đầu (đa dạng trạng thái) với ngày backdate
            LocalDate base = LocalDate.now();
            createOrder(buyer, shop, "SEED-ORD-001", OrderStatus.DELIVERED, new BigDecimal("7990000"), new BigDecimal("35000"), 10.0, products[0], variants[0], 1, new BigDecimal("7990000"),
                    base.minusDays(45).atTime(LocalTime.NOON), base.minusDays(44).atTime(18, 0));
            createOrder(buyer, shop, "SEED-ORD-002", OrderStatus.DELIVERED, new BigDecimal("5990000"), new BigDecimal("30000"), 10.0, products[5], variants[5], 1, new BigDecimal("5990000"),
                    base.minusDays(30).atTime(10, 0), base.minusDays(29).atTime(14, 0));
            createOrder(buyer, shop, "SEED-ORD-003", OrderStatus.CONFIRMED, new BigDecimal("690000"), new BigDecimal("22000"), 10.0, products[7], variants[7], 1, new BigDecimal("690000"),
                    base.minusDays(10).atTime(9, 0), null);
            createOrder(buyer, shop, "SEED-ORD-004", OrderStatus.PROCESSING, new BigDecimal("3290000"), new BigDecimal("28000"), 10.0, products[6], variants[6], 1, new BigDecimal("3290000"),
                    base.minusDays(5).atTime(11, 0), null);
            createOrder(buyer, shop, "SEED-ORD-005", OrderStatus.SHIPPING, new BigDecimal("398000"), new BigDecimal("15000"), 10.0, products[8], variants[8], 2, new BigDecimal("199000"),
                    base.minusDays(3).atTime(8, 0), null);
            createOrder(buyer, shop, "SEED-ORD-006", OrderStatus.PENDING_PAYMENT, new BigDecimal("2490000"), new BigDecimal("25000"), 10.0, products[11], variants[11], 1, new BigDecimal("2490000"),
                    base.minusDays(1).atTime(16, 0), null);
            createOrder(buyer, shop, "SEED-ORD-007", OrderStatus.CANCELLED, new BigDecimal("35990000"), new BigDecimal("50000"), 10.0, products[2], variants[2], 1, new BigDecimal("35990000"),
                    base.minusDays(20).atTime(12, 0), null);

            // Đơn trải đều 90 ngày (DELIVERED) – gấp đôi: ~97 đơn (7 + 97 = 104)
            int orderSeq = 8;
            for (int i = 0; i < 97; i++) {
                int dayOffset = i % 90;
                LocalDate orderDate = base.minusDays(90 - dayOffset);
                LocalDateTime created = orderDate.atTime(10 + (i % 8), (i * 7) % 60);
                LocalDateTime delivered = orderDate.plusDays(1).atTime(15, 0);
                int shopIdx = i % allShops.size();
                Shop orderShop = allShops.get(shopIdx);
                User orderBuyer = demoBuyers.get(i % demoBuyers.size());
                Product[] prods = shopProducts.get(shopIdx);
                if (prods == null || prods.length == 0) continue;
                Product prod = prods[i % prods.length];
                ProductVariant var = productVariantRepository.findByProduct(prod).stream().findFirst().orElse(null);
                if (var == null) continue;
                String ordNum = "SEED-ORD-" + String.format("%03d", orderSeq);
                BigDecimal price = prod.getBasePrice();
                int qty = 1 + (i % 2);
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
                BigDecimal ship = new BigDecimal("25000");
                createOrder(orderBuyer, orderShop, ordNum, OrderStatus.DELIVERED, subtotal, ship, 10.0,
                        prod, var, qty, price, created, delivered);
                orderSeq++;
            }

            log.info("✓ Demo data (gấp đôi): {} shops, {} buyers, 40+12+12+12 products, {} orders (trải 90 ngày).", allShops.size(), demoBuyers.size(), orderSeq - 1);
        } catch (Exception e) {
            log.error("✗ Error creating sample data: {}", e.getMessage(), e);
        }
        log.info("Sample data initialization completed.");
    }

    private static final String SAMPLE_PRODUCT_IMAGE_URL =
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400";

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
        createOrder(buyer, shop, orderNumber, status, subtotal, shippingFee, commissionRate,
                product, variant, qty, unitPrice, null, null);
    }

    /**
     * Tạo đơn với ngày tùy chỉnh (cho demo: trải đơn theo 90 ngày).
     * createdAt/deliveredAt nếu khác null sẽ được cập nhật vào DB sau khi save.
     */
    private void createOrder(User buyer, Shop shop, String orderNumber, OrderStatus status,
                            BigDecimal subtotal, BigDecimal shippingFee, double commissionRate,
                            Product product, ProductVariant variant, int qty, BigDecimal unitPrice,
                            LocalDateTime createdAt, LocalDateTime deliveredAt) {
        if (orderRepository.findByOrderNumber(orderNumber).isPresent()) {
            return;
        }
        BigDecimal ship = shippingFee != null ? shippingFee : BigDecimal.ZERO;

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(buyer);
        order.setShop(shop);
        order.setStatus(status);
        order.setShippingName(buyer.getFullName() != null ? buyer.getFullName() : "Khách hàng");
        order.setShippingPhone("0901234567");
        order.setShippingAddress("123 Đường Mẫu, Quận 1, TP.HCM");
        order.setShippingCity("TP. Hồ Chí Minh");
        order.setSubtotal(subtotal);
        order.setShippingFee(ship);
        order.calculateTotal();
        if (status == OrderStatus.DELIVERED) {
            order.setReceivedByBuyer(true);
            if (deliveredAt != null) order.setDeliveredAt(deliveredAt);
        }
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
        order.addItem(item);
        order = orderRepository.save(order);
        if (status == OrderStatus.DELIVERED) {
            commissionService.createCommission(order);
            if (deliveredAt != null) {
                commissionRepository.updateCreatedAtByOrderId(order.getId(), deliveredAt);
            }
        }
        if (createdAt != null || deliveredAt != null) {
            LocalDateTime c = createdAt != null ? createdAt : order.getCreatedAt();
            LocalDateTime d = deliveredAt != null ? deliveredAt : order.getDeliveredAt();
            LocalDateTime u = (d != null ? d : c);
            orderRepository.updateOrderDates(order.getId(), c, d, u);
        }
        log.info("✓ Created sample order: {} status={}", orderNumber, status);
    }
}
