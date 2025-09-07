package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.RoleRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Get all users with optional filtering and pagination
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers(int page, int size, String search, String role, Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<User> usersPage;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search by email, firstName, or lastName
            usersPage = userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                    search.trim(), search.trim(), search.trim(), pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        
        List<User> users = usersPage.getContent();
        
        // Apply additional filters
        if (role != null && !role.trim().isEmpty()) {
            RoleName roleName = RoleName.valueOf("ROLE_" + role.toUpperCase());
            users = users.stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(r -> r.getName() == roleName))
                    .collect(Collectors.toList());
        }
        
        if (isActive != null) {
            users = users.stream()
                    .filter(user -> user.getActive() == isActive)
                    .collect(Collectors.toList());
        }
        
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        return convertToDTO(user);
    }

    /**
     * Update user status (active/inactive)
     */
    @Transactional
    public UserDTO updateUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        user.setActive(isActive);
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User {} status updated to: {}", userId, isActive);
        
        return convertToDTO(savedUser);
    }

    /**
     * Update user role
     */
    @Transactional
    public UserDTO updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Find the role
        RoleName roleEnum;
        try {
            roleEnum = RoleName.valueOf("ROLE_" + roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role name: " + roleName);
        }
        
        Role role = roleRepository.findByName(roleEnum)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        // Clear existing roles and set new role
        user.getRoles().clear();
        user.getRoles().add(role);
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User {} role updated to: {}", userId, roleName);
        
        return convertToDTO(savedUser);
    }

    @Transactional(readOnly = true)
public Map<String, Object> getUsersStatistics() {
    long totalUsers = userRepository.count();
    long activeUsers = userRepository.countByEnabledTrue();
    long inactiveUsers = userRepository.countByEnabledFalse();

    // Count users by role
    List<User> allUsers = userRepository.findAll();
    Map<String, Long> roleStats = new HashMap<>();

    for (User user : allUsers) {
        for (Role role : user.getRoles()) {
            String roleName = role.getName().name().replace("ROLE_", "");
            roleStats.put(roleName, roleStats.getOrDefault(roleName, 0L) + 1);
        }
    }

    // Recent registrations (last 30 days)
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    long recentRegistrations = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalUsers", totalUsers);
    stats.put("activeUsers", activeUsers);
    stats.put("inactiveUsers", inactiveUsers);
    stats.put("roleStatistics", roleStats);
    stats.put("recentRegistrations", recentRegistrations);

    return stats;
}


    /**
     * Soft delete user (deactivate)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Soft delete - just deactivate the user
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("User {} has been deactivated (soft deleted)", userId);
    }

    /**
     * Convert User entity to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        // Get primary role (first role)
        if (!user.getRoles().isEmpty()) {
            Role primaryRole = user.getRoles().iterator().next();
            dto.setRole(primaryRole.getName().name().replace("ROLE_", ""));
        } else {
            dto.setRole("USER");
        }
        
        return dto;
    }
}
