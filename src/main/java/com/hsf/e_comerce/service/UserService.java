package com.hsf.e_comerce.service;

import com.hsf.e_comerce.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    
    User findById(UUID id);
    
    User findByEmail(String email);
    
    List<String> getUserRoles(UUID userId);
    
    void assignRoleToUser(User user, String roleName);
}
