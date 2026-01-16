package com.hsf.e_comerce.auth.service;

import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    
    User findById(UUID id);
    
    User findByEmail(String email);
    
    List<String> getUserRoles(UUID userId);
    
    void assignRoleToUser(User user, String roleName);
    
    // Admin methods
    List<User> getAllUsers();
    
    List<UserResponse> getAllUserResponses();
    
    UserResponse getUserResponseById(UUID id);
    
    User updateUser(UUID userId, String fullName, String email, String phoneNumber, String roleName, Boolean isActive);
    
    UserResponse updateUserAndGetResponse(UUID userId, String fullName, String email, String phoneNumber, String roleName, Boolean isActive);
    
    void deleteUser(UUID userId);
    
    void activateUser(UUID userId);
    
    void deactivateUser(UUID userId);
}
