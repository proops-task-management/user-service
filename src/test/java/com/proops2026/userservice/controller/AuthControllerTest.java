package com.proops2026.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.userservice.dto.request.LoginRequest;
import com.proops2026.userservice.dto.response.LoginResponse;
import com.proops2026.userservice.exception.InvalidCredentialsException;
import com.proops2026.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void login_correctCredentials_returns200WithTokenAndUser() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt-mock-token")
                .user(LoginResponse.UserInfo.builder()
                        .id("f8f3899f-1b6d-4f5f-9b1f-18ea3c3df12f")
                        .email("member@example.com")
                        .role("member")
                        .build())
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        Map<String, String> body = Map.of(
                "email", "member@example.com",
                "password", "securePassword123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-mock-token"))
                .andExpect(jsonPath("$.user.id").value("f8f3899f-1b6d-4f5f-9b1f-18ea3c3df12f"))
                .andExpect(jsonPath("$.user.email").value("member@example.com"))
                .andExpect(jsonPath("$.user.role").value("member"));
    }

    @Test
    void login_wrongPassword_returns401WithGenericMessage() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("invalid email or password"));

        Map<String, String> body = Map.of(
                "email", "member@example.com",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid email or password"));
    }

    @Test
    void login_unknownEmail_returns401WithSameGenericMessage() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("invalid email or password"));

        Map<String, String> body = Map.of(
                "email", "unknown@example.com",
                "password", "securePassword123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid email or password"));
    }
}
