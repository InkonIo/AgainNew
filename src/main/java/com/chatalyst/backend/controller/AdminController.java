package com.chatalyst.backend.controller;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.dto.UserDTO;
import com.chatalyst.backend.security.services.UserPrincipal;
import com.chatalyst.backend.security.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive) {
        
        log.info("Admin {} requesting users list", userPrincipal.getEmail());
        
        List<UserDTO> users = adminService.getAllUsers(page, size, search, role, isActive);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDTO> getUserById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        
        log.info("Admin {} requesting user details for ID: {}", userPrincipal.getEmail(), userId);
        
        UserDTO user = adminService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user status (admin only)
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> statusUpdate) {
        
        Boolean isActive = statusUpdate.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Admin {} updating user {} status to: {}", 
                userPrincipal.getEmail(), userId, isActive);
        
        UserDTO updatedUser = adminService.updateUserStatus(userId, isActive);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     // Update user role (admin only)     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId,
            @RequestBody Map<String, String> roleUpdate) {
        
        String newRole = roleUpdate.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Admin {} updating user {} role to: {}", 
                userPrincipal.getEmail(), userId, newRole);
        
        UserDTO updatedUser = adminService.updateUserRole(userId, newRole);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Get users statistics (admin only)
     */
    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Object>> getUsersStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Admin {} requesting users statistics", userPrincipal.getEmail());
        
        Map<String, Object> stats = adminService.getUsersStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Delete user (admin only) - soft delete
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId) {
        
        log.info("Admin {} deleting user: {}", userPrincipal.getEmail(), userId);
        
        adminService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}
