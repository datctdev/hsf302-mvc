package com.hsf.e_comerce.config;

import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.product.entity.ProductCategory;
import com.hsf.e_comerce.product.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        createDefaultUser("seller@gmail.com", "seller123@", "Người Bán", "ROLE_SELLER");
        
        // Tạo tài khoản ADMIN
        createDefaultUser("admin@gmail.com", "admin123@", "Quản Trị Viên", "ROLE_ADMIN");
        
        log.info("Default users initialization completed.");

        log.info("cai nay de test");
    }

    private void createDefaultUser(String email, String password, String fullName, String roleName) {
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
                user.setRole(role); // Set role cho user
                
                user = userRepository.save(user);
                
                // Verify role was set correctly
                if (user.getRole() != null && user.getRole().getName().equals(roleName)) {
                    log.info("✓ Created user: {} ({}) with role {}", email, fullName, roleName);
                } else {
                    log.warn("⚠ User created but role verification failed: {} (expected: {})", 
                            user.getRole() != null ? user.getRole().getName() : "null", roleName);
                }
            } else {
                // User đã tồn tại, kiểm tra và update role nếu cần
                User existingUser = userRepository.findByEmail(email)
                        .orElse(null);
                if (existingUser != null) {
                    if (existingUser.getRole() == null || !existingUser.getRole().getName().equals(roleName)) {
                        existingUser.setRole(role);
                        userRepository.save(existingUser);
                        log.info("✓ Updated existing user: {} with role {}", email, roleName);
                    } else {
                        log.info("→ User already exists with correct role: {} ({})", email, roleName);
                    }
                } else {
                    log.warn("→ User exists but could not be loaded: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("✗ Error creating/updating user {}: {}", email, e.getMessage(), e);
        }
    }

    private void initializeProductCategories() {
        log.info("Starting product categories initialization...");
        
        // Định nghĩa cấu trúc categories (parent -> children)
        Map<String, List<String>> categoryStructure = new HashMap<>();
        categoryStructure.put("Điện Tử", Arrays.asList("Điện Thoại", "Laptop", "Máy Tính Bảng", "Tai Nghe", "Loa"));
        categoryStructure.put("Thời Trang", Arrays.asList("Quần Áo Nam", "Quần Áo Nữ", "Giày Dép", "Túi Xách", "Đồng Hồ"));
        categoryStructure.put("Đồ Gia Dụng", Arrays.asList("Nội Thất", "Đồ Trang Trí", "Đồ Dùng Nhà Bếp", "Đồ Dùng Phòng Tắm"));
        categoryStructure.put("Sách", Arrays.asList("Sách Văn Học", "Sách Kỹ Thuật", "Sách Thiếu Nhi", "Truyện Tranh"));
        categoryStructure.put("Thể Thao", Arrays.asList("Dụng Cụ Thể Thao", "Quần Áo Thể Thao", "Giày Thể Thao"));
        categoryStructure.put("Mỹ Phẩm", Arrays.asList("Chăm Sóc Da", "Trang Điểm", "Nước Hoa", "Chăm Sóc Tóc"));
        categoryStructure.put("Đồ Chơi", Arrays.asList("Đồ Chơi Trẻ Em", "Mô Hình", "Board Game"));
        
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
        log.info("Product categories initialization completed.");
    }
}
