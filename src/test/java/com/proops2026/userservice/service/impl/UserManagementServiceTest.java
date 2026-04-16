package com.proops2026.userservice.service.impl;

import com.proops2026.userservice.dto.request.CreateManagedUserRequest;
import com.proops2026.userservice.dto.request.UpdateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.exception.BadRequestException;
import com.proops2026.userservice.exception.UnauthorizedException;
import com.proops2026.userservice.exception.UserAlreadyExistsException;
import com.proops2026.userservice.mapper.UserMapper;
import com.proops2026.userservice.model.User;
import com.proops2026.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        userService = new UserServiceImpl(userRepository, passwordEncoder, userMapper);
    }

    @Test
    void createManagedUser_withLeadRole_persistsNormalizedRole() {
        CreateManagedUserRequest request = new CreateManagedUserRequest();
        request.setEmail("lead@example.com");
        request.setPassword("securePassword123");
        request.setRole("LEAD");

        when(userRepository.existsByEmail("lead@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createManagedUser("lead", request);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo("lead");
        assertThat(response.getRole()).isEqualTo("lead");
    }

    @Test
    void createManagedUser_withoutLeadRole_rejectsRequest() {
        CreateManagedUserRequest request = new CreateManagedUserRequest();
        request.setEmail("member@example.com");
        request.setPassword("securePassword123");

        assertThatThrownBy(() -> userService.createManagedUser("member", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("lead role required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void listUsers_withLeadRole_returnsMappedUsers() {
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(
                User.builder()
                        .id("user-1")
                        .email("lead@example.com")
                        .passwordHash("hash")
                        .role("lead")
                        .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                        .build()
        ));

        List<UserResponse> response = userService.listUsers("lead");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getEmail()).isEqualTo("lead@example.com");
    }

    @Test
    void updateUser_withoutAnyFields_rejectsRequest() {
        UpdateUserRequest request = new UpdateUserRequest();

        assertThatThrownBy(() -> userService.updateUser("lead", "user-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("at least one field must be provided");
    }

    @Test
    void updateUser_updatesEmailPasswordAndRole() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@example.com");
        request.setPassword("newPassword123");
        request.setRole("LEAD");

        User existingUser = User.builder()
                .id("user-1")
                .email("old@example.com")
                .passwordHash("old-hash")
                .role("member")
                .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser("lead", "user-1", request);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("updated@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("new-hash");
        assertThat(saved.getRole()).isEqualTo("lead");
        assertThat(response.getRole()).isEqualTo("lead");
    }

    @Test
    void updateUser_duplicateEmail_rejectsRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("duplicate@example.com");

        User existingUser = User.builder()
                .id("user-1")
                .email("old@example.com")
                .passwordHash("old-hash")
                .role("member")
                .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(
                User.builder()
                        .id("user-2")
                        .email("duplicate@example.com")
                        .passwordHash("hash")
                        .role("member")
                        .createdAt(LocalDateTime.parse("2026-04-16T09:00:00"))
                        .build()
        ));

        assertThatThrownBy(() -> userService.updateUser("lead", "user-1", request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("email already in use");
    }
}
