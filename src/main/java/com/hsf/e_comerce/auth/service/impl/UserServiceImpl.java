package com.hsf.e_comerce.auth.service.impl;

import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.entity.Role;
import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.repository.RoleRepository;
import com.hsf.e_comerce.auth.repository.UserRepository;
import com.hsf.e_comerce.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        if (user.getRole() != null) {
            return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName()));
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public User findById(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public User findByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmailWithRole(String email) {
        return userRepository.findByEmailAndDeletedFalseWithRole(email)
                .orElse(null); // Return null instead of throwing exception for GlobalControllerAdvice
    }

    @Override
    @Transactional
    public List<String> getUserRoles(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if (user.getRole() != null) {
            return Collections.singletonList(user.getRole().getName());
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void assignRoleToUser(User user, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findByDeletedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUserResponses() {
        return userRepository.findByDeletedFalse().stream()
                .map(user -> UserResponse.convertToResponse(user, this))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(UUID id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.convertToResponse(user, this);
    }

    @Override
    @Transactional
    public User updateUser(UUID userId, String fullName, String email, String phoneNumber, String roleName, Boolean isActive) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }
        
        if (email != null && !email.isEmpty()) {
            // Check if email already exists for another user (not deleted)
            userRepository.findByEmailAndDeletedFalse(email).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                    throw new RuntimeException("Email đã được sử dụng bởi người dùng khác");
                }
            });
            user.setEmail(email);
        }
        
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
        
        if (roleName != null && !roleName.isEmpty()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            user.setRole(role);
        }
        
        if (isActive != null) {
            user.setIsActive(isActive);
        }
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserAndGetResponse(UUID userId, String fullName, String email, String phoneNumber, String roleName, Boolean isActive) {
        User user = updateUser(userId, fullName, email, phoneNumber, roleName, isActive);
        return UserResponse.convertToResponse(user, this);
    }

    @Override
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
    }
}
