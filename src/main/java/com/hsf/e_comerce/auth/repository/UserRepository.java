package com.hsf.e_comerce.auth.repository;

import com.hsf.e_comerce.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndDeletedFalse(String email);
    
    @EntityGraph(attributePaths = {"role"})
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailAndDeletedFalseWithRole(@Param("email") String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndDeletedFalse(String email);
    
    List<User> findByDeletedFalse();
    
    Optional<User> findByIdAndDeletedFalse(UUID id);
    
    @Query("SELECT u FROM User u WHERE u.deleted = false")
    List<User> findAllNotDeleted();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

}
