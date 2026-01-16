package com.hsf.e_comerce.config;

import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeDefaultUsers();
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
    }

    private void createDefaultUser(String email, String password, String fullName, String roleName) {
        try {
            if (!userRepository.existsByEmail(email)) {
                // Lấy role
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                
                // Tạo user với role
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setFullName(fullName);
                user.setIsActive(true);
                user.setRole(role);
                user = userRepository.save(user);
                
                log.info("✓ Created user: {} ({}) with role {}", email, fullName, roleName);
            } else {
                log.info("→ User already exists: {}", email);
            }
        } catch (Exception e) {
            log.error("✗ Error creating user {}: {}", email, e.getMessage(), e);
        }
    }
}
