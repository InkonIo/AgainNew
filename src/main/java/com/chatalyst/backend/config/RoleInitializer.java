package com.chatalyst.backend.config;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Базовые роли
        createRoleIfNotExists(RoleName.ROLE_USER);
        createRoleIfNotExists(RoleName.ROLE_ADMIN);
        
        // Роли Standard подписки
        createRoleIfNotExists(RoleName.ROLE_STANDARD_1M);
        createRoleIfNotExists(RoleName.ROLE_STANDARD_3M);
        createRoleIfNotExists(RoleName.ROLE_STANDARD_6M);
        createRoleIfNotExists(RoleName.ROLE_STANDARD_12M);
        
        // Роли Premium подписки
        createRoleIfNotExists(RoleName.ROLE_PREMIUM_1M);
        createRoleIfNotExists(RoleName.ROLE_PREMIUM_3M);
        createRoleIfNotExists(RoleName.ROLE_PREMIUM_6M);
        createRoleIfNotExists(RoleName.ROLE_PREMIUM_12M);
        
        // Роль для скидки после пробного месяца
        createRoleIfNotExists(RoleName.ROLE_AFTERMONTH_DISCOUNT);
    }

    private void createRoleIfNotExists(RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            System.out.println("Создана роль: " + roleName);
        }
    }
}
