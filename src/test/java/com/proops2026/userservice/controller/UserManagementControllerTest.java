package com.proops2026.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.userservice.dto.request.CreateManagedUserRequest;
import com.proops2026.userservice.dto.request.UpdateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.exception.UnauthorizedException;
import com.proops2026.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void listUsers_leadRole_returnsUsers() throws Exception {
        List<UserResponse> response = List.of(
                UserResponse.builder()
                        .id("user-1")
                        .email("lead@example.com")
                        .role("lead")
                        .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                        .build(),
                UserResponse.builder()
                        .id("user-2")
                        .email("member@example.com")
                        .role("member")
                        .createdAt(LocalDateTime.parse("2026-04-16T08:00:00"))
                        .build()
        );
        when(userService.listUsers("lead")).thenReturn(response);

        mockMvc.perform(get("/users").header("X-User-Role", "lead"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[1].role").value("member"));
    }

    @Test
    void listUsers_memberRole_returns403() throws Exception {
        when(userService.listUsers("member"))
                .thenThrow(new UnauthorizedException("lead role required"));

        mockMvc.perform(get("/users").header("X-User-Role", "member"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("lead role required"));
    }

    @Test
    void createManagedUser_validRequest_returns201() throws Exception {
        UserResponse response = UserResponse.builder()
                .id("managed-1")
                .email("managed@example.com")
                .role("lead")
                .createdAt(LocalDateTime.parse("2026-04-16T10:00:00"))
                .build();
        when(userService.createManagedUser(eq("lead"), any(CreateManagedUserRequest.class))).thenReturn(response);

        Map<String, String> body = Map.of(
                "email", "managed@example.com",
                "password", "securePassword123",
                "role", "lead"
        );

        mockMvc.perform(post("/users/admin")
                        .header("X-User-Role", "lead")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("managed-1"))
                .andExpect(jsonPath("$.role").value("lead"));
    }

    @Test
    void updateUser_validRequest_returnsUpdatedUser() throws Exception {
        UserResponse response = UserResponse.builder()
                .id("user-1")
                .email("updated@example.com")
                .role("lead")
                .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                .build();
        when(userService.updateUser(eq("lead"), eq("user-1"), any(UpdateUserRequest.class))).thenReturn(response);

        Map<String, String> body = Map.of(
                "email", "updated@example.com",
                "role", "lead"
        );

        mockMvc.perform(patch("/users/user-1")
                        .header("X-User-Role", "lead")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("lead"));
    }

    @Test
    void deleteUser_validRequest_returns204() throws Exception {
        mockMvc.perform(delete("/users/user-1").header("X-User-Role", "lead"))
                .andExpect(status().isNoContent());
    }
}
