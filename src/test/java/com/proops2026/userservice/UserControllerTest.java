package com.proops2026.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=test-secret-key-at-least-32-characters-long"
)
@Testcontainers
class UserControllerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    // -----------------------------------------------------------------------
    // Test 1: valid registration → 201 with id, email, created_at
    // -----------------------------------------------------------------------
    @Test
    void register_validRequest_returns201WithUserData() {
        Map<String, String> body = Map.of(
                "email", "minh@example.com",
                "password", "securePassword123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("email");
        assertThat(response.getBody()).containsKey("created_at");
        assertThat(response.getBody().get("email")).isEqualTo("minh@example.com");
    }

    // -----------------------------------------------------------------------
    // Test 2: duplicate email → 409 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_duplicateEmail_returns409() {
        Map<String, String> body = Map.of(
                "email", "duplicate@example.com",
                "password", "securePassword123"
        );

        // first registration — should succeed
        restTemplate.postForEntity("/users", body, Map.class);

        // second registration with same email — should fail
        ResponseEntity<Map> response = restTemplate.postForEntity("/users", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).isEqualTo("email already in use");
    }

    // -----------------------------------------------------------------------
    // Test 3: missing email → 400 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_missingEmail_returns400() {
        Map<String, String> body = Map.of(
                "password", "securePassword123"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("email is required");
    }

    // -----------------------------------------------------------------------
    // Test 4: password shorter than 8 characters → 400 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_shortPassword_returns400() {
        Map<String, String> body = Map.of(
                "email", "test@example.com",
                "password", "short"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/users", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("password must be at least 8 characters");
    }
}
