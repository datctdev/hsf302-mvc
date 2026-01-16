package com.hsf.e_comerce.auth.controller;

import com.hsf.e_comerce.auth.dto.request.UpdateUserRequest;
import com.hsf.e_comerce.auth.dto.response.UserResponse;
import com.hsf.e_comerce.auth.service.UserService;
import com.hsf.e_comerce.common.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUserResponses();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserResponseById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUserAndGetResponse(
                id,
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getRoleName(),
                request.getIsActive()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Xóa người dùng thành công")
                .build());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Kích hoạt tài khoản thành công")
                .build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Vô hiệu hóa tài khoản thành công")
                .build());
    }
}
