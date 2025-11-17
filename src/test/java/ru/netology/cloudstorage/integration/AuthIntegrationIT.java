package ru.netology.cloudstorage.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.netology.cloudstorage.model.dto.LoginRequest;
import ru.netology.cloudstorage.model.dto.LoginResponse;
import ru.netology.cloudstorage.model.entity.User;
import ru.netology.cloudstorage.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthIntegrationIT {
    private static final String TEST_LOGIN = "testlogin";
    private static final String RAW_PASSWORD = "testpassword";

    // ... (PostgreSQLContainer и DynamicPropertySource)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("integration_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeAll
    static void setup() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void login_SuccessfulAuthentication_ReturnsAuthToken() {
        // Подготовка данных
        userRepository.deleteAll();
        User user = new User(); // Используем TEST_LOGIN, который соответствует полю username в сущности
        user.setUsername(TEST_LOGIN);
        user.setEmail(TEST_LOGIN + "@test.com");
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        userRepository.save(user);

        // Создание запроса и явное указание Content-Type
        LoginRequest request = new LoginRequest(TEST_LOGIN, RAW_PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers); // Оборачиваем DTO в HttpEntity

        // Вызов эндпоинта с HttpEntity
//        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/cloud/login", entity, LoginResponse.class);

        ResponseEntity<String> debugResp = restTemplate.postForEntity("/login", entity, String.class);
        System.out.println("DEBUG STATUS: " + debugResp.getStatusCode());
        System.out.println("DEBUG BODY: " + debugResp.getBody());

        if (debugResp.getStatusCode() == HttpStatus.OK) {
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/login", entity, LoginResponse.class);

            // Проверки
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getAuthToken(), "Токен должен быть возвращен");
            //
        }else {
            fail("Login failed: " + debugResp.getStatusCode() + " body=" + debugResp.getBody());
        }

    }

    @Test
    void login_Failure_ReturnsUnauthorizedStatus() {
        userRepository.deleteAll(); // убедимся, что пользователя нет

        // Создание запроса и явное указание Content-Type
        LoginRequest request = new LoginRequest("nonExistentLogin", "wrongPass");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        // Вызов эндпоинта с HttpEntity
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/login", entity, LoginResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
