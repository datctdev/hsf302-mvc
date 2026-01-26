package com.hsf.e_comerce.config;

import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import com.hsf.e_comerce.shop.entity.Shop;
import com.hsf.e_comerce.shop.repository.ShopRepository;
import com.hsf.e_comerce.shop.valueobject.ShopStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductCategoryRepository categoryRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeDefaultUsers();
        initializeProductCategories();
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
}
