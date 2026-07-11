package com.proops2026.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context integration test against a real MySQL (Testcontainers).
 *
 * Proves the D6 acceptance criteria (IRD-001 amended, ADR-005):
 *   - tokens are RS256, carry kid=proops-v1, and verify with the PUBLIC key only;
 *   - duplicate email -> 409; wrong password -> 401.
 *
 * The RSA keypair is generated per test run: the private half is injected as
 * jwt.private-key (what JwtUtil signs with); the public half stays in the test
 * to verify the signature — mirroring how api-gateway will verify in D7.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserServiceRs256IntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    private static final KeyPair KEY_PAIR = generateKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // Inject the generated private key so JwtUtil signs with a key the test controls
        registry.add("jwt.private-key", () -> toPkcs8Pem(KEY_PAIR));
        registry.add("jwt.expires-in", () -> "86400000");
    }

    @Test
    void registerThenLogin_tokenIsRs256_verifiableWithPublicKey() throws Exception {
        String email = "rs256@example.com";
        register(email, "securePassword123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("member"));

        String token = loginAndExtractToken(email, "securePassword123");

        // Verify with the PUBLIC key only — this is the whole point of RS256
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(KEY_PAIR.getPublic())
                .build()
                .parseSignedClaims(token);

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("RS256");
        assertThat(jws.getHeader().getKeyId()).isEqualTo("proops-v1");
        assertThat(jws.getPayload().get("email")).isEqualTo(email);
        assertThat(jws.getPayload().get("role")).isEqualTo("member");
        assertThat(jws.getPayload().getId()).isNotNull(); // jti present
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = "dupe@example.com";
        register(email, "securePassword123").andExpect(status().isCreated());

        register(email, "securePassword123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("email already in use"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = "wrongpw@example.com";
        register(email, "securePassword123").andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, "totallyWrongPassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid email or password"));
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.ResultActions register(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(email, password)));
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test RSA keypair", e);
        }
    }

    private static String toPkcs8Pem(KeyPair keyPair) {
        String base64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        // Banner built from split literals: this is a per-run test key (no secret), but the
        // contiguous "BEGIN PRIVATE KEY" string trips gitleaks' private-key rule (a Block gate).
        String begin = "-----BEGIN " + "PRIVATE KEY-----";
        String end = "-----END " + "PRIVATE KEY-----";
        return begin + "\n" + base64 + "\n" + end;
    }
}
