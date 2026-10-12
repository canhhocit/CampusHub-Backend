package com.campushub.common.bootstrap;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            logger.info("Admin bootstrap variables not set. Skipping admin seeder.");
            return;
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setUsername("admin"); // Can be configured further if needed
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setIdentityVerified(true);

            // Avoid username collision
            if (userRepository.existsByUsername("admin")) {
                admin.setUsername("admin_" + System.currentTimeMillis());
            }

            userRepository.save(admin);
            logger.info("Bootstrap: Admin user created successfully.");
        } else {
            logger.info("Bootstrap: Admin user already exists. Skipping.");
        }
    }
}
