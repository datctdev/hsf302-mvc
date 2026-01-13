package com.hsf.e_comerce.auth.service;

import com.hsf.e_comerce.auth.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    
    User findById(UUID id);
    
    User findByEmail(String email);
    
    List<String> getUserRoles(UUID userId);
    
    void assignRoleToUser(User user, String roleName);
}
