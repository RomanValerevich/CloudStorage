package ru.netology.cloudstorage.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.netology.cloudstorage.dto.LoginResponse;

import static org.junit.jupiter.api.Assertions.*;

public class LoginResponseSerializationTest {
    @Test
    void loginResponse_ShouldSerializeToAuthTokenKebabCase() throws Exception {
        // Предполагаем, что LoginResponse использует @JsonProperty("auth-token")
        LoginResponse resp = new LoginResponse("test-jwt-token-123");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(resp);

        // Проверяем, что JSON содержит ключ "auth-token"
        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("auth-token"), "JSON должен содержать ключ 'auth-token'");
        assertEquals("test-jwt-token-123", node.get("auth-token").asText());

        // Проверяем, что нет ошибочного ключа "authToken"
        assertFalse(node.has("authToken"), "JSON не должен содержать ключ 'authToken'");
    }
}
