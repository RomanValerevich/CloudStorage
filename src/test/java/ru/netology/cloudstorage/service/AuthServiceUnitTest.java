package ru.netology.cloudstorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.cloudstorage.exception.BadRequestException;
import ru.netology.cloudstorage.model.entity.User;
import ru.netology.cloudstorage.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {
    @Mock
    private UserRepository userRepository; // Мокируем зависимость

    @Mock
    private PasswordEncoder passwordEncoder; // Мокируем PasswordEncoder

    @InjectMocks
    private AuthServiceImpl authService; // Внедряем моки в тестируемый класс

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password";


    @Test
    void login_Success_ReturnsToken() {
        // 1. Настройка мока: когда репозиторий ищет пользователя, вернуть его
        User mockUser = new User();
        mockUser.setEmail(TEST_EMAIL);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash(TEST_PASSWORD);

        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.of(mockUser));

        // Настраиваем PasswordEncoder.matches: для правильного пароля возвращаем true
        when(passwordEncoder.matches(TEST_PASSWORD, mockUser.getPasswordHash()))
                .thenReturn(true);
        String token = authService.login(TEST_EMAIL, TEST_PASSWORD);

        assertNotNull(token);
        assertTrue(token.length() > 10); // Проверка, что токен сгенерирован
    }

    @Test
    void login_Failure_ThrowsUnauthorizedException() {
        // 1. Настройка мока: пользователь не найден
        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.empty());

        // 2. Проверка исключения
        assertThrows(BadRequestException.class, () -> {
            authService.login(TEST_EMAIL, TEST_PASSWORD);
        });
    }
}
