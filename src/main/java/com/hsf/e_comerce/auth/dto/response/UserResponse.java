package com.hsf.e_comerce.auth.dto.response;

import com.hsf.e_comerce.auth.entity.User;
import com.hsf.e_comerce.auth.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private List<String> roles;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static UserResponse convertToResponse(User user, UserService userService) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .roles(userService.getUserRoles(user.getId()))
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
