package ru.netology.cloudstorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.cloudstorage.model.User;
import ru.netology.cloudstorage.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DataSeeder {

    private static final String DEFAULT_DOMAIN = "@test.com";

    @Bean
    public ApplicationRunner seedAdminUser(
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password:}") String adminPassword,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        log.info("DataSeeder: Starting user initialization");
        log.info("Admin username: {}", adminUsername);
        return args -> {
            // Администратор
            if (adminUsername != null && !adminUsername.isBlank() &&
                    adminPassword != null && !adminPassword.isBlank()) {

                String email = adminUsername.contains("@") ? adminUsername : adminUsername + DEFAULT_DOMAIN;

                userRepository.findByEmail(email).ifPresentOrElse(
                        existingUser -> {
                            // Обновляем существующего администратора
                            if (!passwordEncoder.matches(adminPassword, existingUser.getPasswordHash())) {
                                existingUser.setPasswordHash(passwordEncoder.encode(adminPassword));
                                userRepository.save(existingUser);
                            }
                        },
                        () -> {
                            // Создаем нового администратора
                            User admin = new User();
                            admin.setUsername(adminUsername);
                            admin.setEmail(email);
                            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                            userRepository.save(admin);
                        }
                );
            }

            // Тестовые пользователи
            createUserIfNotExists("user1", "user1@test.com", "password1", userRepository, passwordEncoder);
            createUserIfNotExists("user2", "user2@test.com", "password2", userRepository, passwordEncoder);
        };
    }

    private void createUserIfNotExists(String username, String email, String password,
                                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        log.info("Creating test user: {}, email: {}", username, email);
        userRepository.findByEmail(email).or(() -> userRepository.findByUsername(username))
                .ifPresentOrElse(
                        existingUser -> {
                            // Обновляем пользователя, если изменился пароль
                            if (!passwordEncoder.matches(password, existingUser.getPasswordHash())) {
                                existingUser.setPasswordHash(passwordEncoder.encode(password));
                                existingUser.setEmail(email); // Обновляем email
                                userRepository.save(existingUser);
                            }
                        },
                        () -> {
                            // Создаем нового пользователя
                            User user = new User();
                            user.setUsername(username);
                            user.setEmail(email);
                            user.setPasswordHash(passwordEncoder.encode(password));
                            userRepository.save(user);
                        }
                );
    }
}
