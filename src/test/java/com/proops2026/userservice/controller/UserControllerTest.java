package com.proops2026.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.userservice.controller.UserController;
import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.exception.UserAlreadyExistsException;
import com.proops2026.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // -----------------------------------------------------------------------
    // Test 1: valid registration → 201 with id, email, created_at
    // -----------------------------------------------------------------------
    @Test
    void register_validRequest_returns201WithUserData() throws Exception {
    UserResponse response = UserResponse.builder()
        .id("f8f3899f-1b6d-4f5f-9b1f-18ea3c3df12f")
        .email("minh@example.com")
        .createdAt(LocalDateTime.parse("2026-04-15T10:30:00"))
        .build();
    when(userService.register(any(CreateUserRequest.class))).thenReturn(response);

        Map<String, String> body = Map.of(
                "email", "minh@example.com",
                "password", "securePassword123"
        );

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("f8f3899f-1b6d-4f5f-9b1f-18ea3c3df12f"))
        .andExpect(jsonPath("$.email").value("minh@example.com"))
        .andExpect(jsonPath("$.created_at").value("2026-04-15T10:30:00"));
    }

    // -----------------------------------------------------------------------
    // Test 2: duplicate email → 409 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_duplicateEmail_returns409() throws Exception {
    when(userService.register(any(CreateUserRequest.class)))
        .thenThrow(new UserAlreadyExistsException("email already in use"));

        Map<String, String> body = Map.of(
                "email", "duplicate@example.com",
                "password", "securePassword123"
        );

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("email already in use"));
    }

    // -----------------------------------------------------------------------
    // Test 3: missing email → 400 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_missingEmail_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "password", "securePassword123"
        );

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("email is required"));
    }

    // -----------------------------------------------------------------------
    // Test 4: password shorter than 8 characters → 400 with error message
    // -----------------------------------------------------------------------
    @Test
    void register_shortPassword_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "email", "test@example.com",
                "password", "short"
        );

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password must be at least 8 characters"));
    }
}
