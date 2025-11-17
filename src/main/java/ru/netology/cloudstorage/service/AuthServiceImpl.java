package ru.netology.cloudstorage.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.cloudstorage.exception.BadRequestException;
import ru.netology.cloudstorage.exception.UnauthorizedException;
import ru.netology.cloudstorage.model.entity.User;
import ru.netology.cloudstorage.repository.UserRepository;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public String login(String login, String rawPassword) {
        // Сначала попробуем найти по электронной почте, затем по имени пользователя для обратной совместимости
        User user = userRepository.findByEmail(login)
                .orElseGet(() -> userRepository.findByUsername(login)
                        .orElseThrow(() -> new BadRequestException("Пользователь с таким email/логином не найден")));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadRequestException("Неверный пароль");
        }

        String newToken = UUID.randomUUID().toString();
        user.setCurrentAuthToken(newToken);
        userRepository.save(user);
        return newToken;
    }

    @Transactional
    @Override
    public void logout(String authToken) {
        userRepository.findByCurrentAuthToken(authToken)
                .ifPresent(user -> {
                    user.setCurrentAuthToken(null);
                    userRepository.save(user);
                });
    }

    @Transactional
    @Override
    public String validateTokenAndGetUsername(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new UnauthorizedException("Ошибка авторизации: отсутствует токен авторизации");
        }

        String normalized = authToken.startsWith("Bearer ") ? authToken.substring(7) : authToken;

        User user = userRepository.findByCurrentAuthToken(normalized)
                .orElseThrow(() -> new UnauthorizedException("Неверный или устаревший токен авторизации"));

        // Возвращается адрес электронной почты вместо имени пользователя для аутентификации
        return user.getEmail() != null ? user.getEmail() : user.getUsername();
    }
    
    @Transactional
    @Override
    public void register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        userRepository.save(user);
    }

}
