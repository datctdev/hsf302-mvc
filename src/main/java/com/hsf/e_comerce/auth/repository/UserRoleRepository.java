package com.hsf.e_comerce.auth.repository;

import com.hsf.e_comerce.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, com.hsf.e_comerce.auth.entity.UserRoleId> {
    
    List<UserRole> findByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
}
