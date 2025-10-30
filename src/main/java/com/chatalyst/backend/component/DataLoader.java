package com.chatalyst.backend.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.RoleRepository;
import com.chatalyst.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.user1.email}")
    private String admin1Email;
    @Value("${admin.user1.password}")
    private String admin1Password;

    @Value("${admin.user2.email}")
    private String admin2Email;
    @Value("${admin.user2.password}")
    private String admin2Password;
    
    @Override
    public void run(String... args) throws Exception {
        loadRoles();
        loadAdminUsers();
    }
    
    private void loadRoles() {
        if (roleRepository.count() == 0) {
            log.info("Loading roles...");
            
            Role userRole = new Role(RoleName.ROLE_USER);
            Role adminRole = new Role(RoleName.ROLE_ADMIN);
            
            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            
            log.info("Roles loaded successfully");
        }
    }
    
    private void loadAdminUsers() {
        log.info("Checking and ensuring admin users...");
            
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
        
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User role not found"));
        
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminRoles.add(userRole);
        
        // Admin 1
        ensureAdminUser(admin1Email, admin1Password, "Admin1", "User", adminRoles);
        
        // Admin 2
        ensureAdminUser(admin2Email, admin2Password, "Admin2", "User", adminRoles);
        
        log.info("Admin users ensured successfully");
    }

    private void ensureAdminUser(String email, String rawPassword, String firstName, String lastName, Set<Role> adminRoles) {
        User user = userRepository.findByEmail(email)
                .orElseGet(User::new);

        boolean isNewUser = user.getId() == null;

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(adminRoles);
        
        // ВАЖНО: Админам 50 ботов и supportLevel = ADMIN
        user.setBotsAllowed(50);
        user.setMonthlyMessagesLimit(999999); // Неограниченно
        user.setSupportLevel("ADMIN");

        userRepository.save(user);

        if (isNewUser) {
            log.info("Admin user created: {} with 50 bots allowed", email);
        } else {
            log.info("Admin user updated (50 bots, ADMIN support level): {}", email);
        }
    }
}