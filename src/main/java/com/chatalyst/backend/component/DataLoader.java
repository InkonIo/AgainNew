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

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
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
        if (userRepository.count() == 0) {
            log.info("Loading admin users...");
            
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("User role not found"));
            
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            adminRoles.add(userRole);
            
            // Admin 1
            if (userRepository.findByEmail("inkonio@bk.ru").isEmpty()) {
                User admin1 = new User();
                admin1.setEmail("inkonio@bk.ru");
                admin1.setPassword(passwordEncoder.encode("Alikhancool20!"));
                admin1.setFirstName("Alikhan");
                admin1.setLastName("Admin");
                admin1.setRoles(adminRoles);
                userRepository.save(admin1);
                log.info("Admin user 1 created: inkonio@bk.ru");
            }
            
            // Admin 2
            if (userRepository.findByEmail("hamzanusyr505@bk.ru").isEmpty()) {
                User admin2 = new User();
                admin2.setEmail("hamzanusyr505@bk.ru");
                admin2.setPassword(passwordEncoder.encode("NzKz.0706$"));
                admin2.setFirstName("Hamza");
                admin2.setLastName("Admin");
                admin2.setRoles(new HashSet<>(adminRoles));
                userRepository.save(admin2);
                log.info("Admin user 2 created: hamzanusyr505@bk.ru");
            }
            
            log.info("Admin users loaded successfully");
        }
    }
}