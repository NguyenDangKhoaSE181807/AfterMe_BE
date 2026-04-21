package com.example.reminder.config;

import com.example.reminder.domain.enums.TonePreference;
import com.example.reminder.domain.enums.UserRole;
import com.example.reminder.domain.enums.UserStatus;
import com.example.reminder.entity.User;
import com.example.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Slf4j
@Profile("!test")
public class DataInitializationConfig {

    @Bean
    public CommandLineRunner initializeData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            try {
                // Check if test data already exists
                if (userRepository.count() > 0) {
                    log.info("Database already contains data, skipping initialization");
                    return;
                }

                log.info("Initializing basic test data...");

                // Create admin user
                User adminUser = User.builder()
                        .email("admin@reminder.local")
                        .passwordHash(passwordEncoder.encode("Admin@123456"))
                        .fullName("Administrator User")
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .tonePreference(TonePreference.NORMAL)
                        .build();
                userRepository.save(adminUser);
                log.info("✓ Created admin user: admin@reminder.local (password: Admin@123456)");

                // Create customer user
                User customerUser = User.builder()
                        .email("customer@reminder.local")
                        .passwordHash(passwordEncoder.encode("Customer@123456"))
                        .fullName("John Customer")
                        .role(UserRole.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .tonePreference(TonePreference.NORMAL)
                        .build();
                userRepository.save(customerUser);
                log.info("✓ Created customer user: customer@reminder.local (password: Customer@123456)");

                // Create consultant user
                User consultantUser = User.builder()
                        .email("consultant@reminder.local")
                        .passwordHash(passwordEncoder.encode("Consultant@123456"))
                        .fullName("Jane Consultant")
                        .role(UserRole.CONSULTANT)
                        .status(UserStatus.ACTIVE)
                        .tonePreference(TonePreference.NORMAL)
                        .build();
                userRepository.save(consultantUser);
                log.info("✓ Created consultant user: consultant@reminder.local (password: Consultant@123456)");

                log.info("Data initialization completed successfully!");
            } catch (Exception ex) {
                log.warn("Could not initialize test data: {}", ex.getMessage());
                log.debug("Full error:", ex);
            }
        };
    }
}
