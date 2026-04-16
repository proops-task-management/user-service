package com.proops2026.userservice.service.impl;

import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.request.CreateManagedUserRequest;
import com.proops2026.userservice.dto.request.UpdateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.exception.BadRequestException;
import com.proops2026.userservice.exception.UnauthorizedException;
import com.proops2026.userservice.exception.UserAlreadyExistsException;
import com.proops2026.userservice.exception.UserNotFoundException;
import com.proops2026.userservice.mapper.UserMapper;
import com.proops2026.userservice.model.User;
import com.proops2026.userservice.repository.UserRepository;
import com.proops2026.userservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(CreateUserRequest request) {
        validateEmailNotTaken(normalizeEmail(request.getEmail()));
        User saved = userRepository.save(buildUser(request.getEmail(), request.getPassword(), "member"));
        log.info("User registered: {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createManagedUser(String requesterRole, CreateManagedUserRequest request) {
        requireLead(requesterRole);
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateEmailNotTaken(normalizedEmail);
        User saved = userRepository.save(buildUser(request.getEmail(), request.getPassword(), request.getRole()));
        log.info("Managed user created: {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Override
    public List<UserResponse> listUsers(String requesterRole) {
        requireLead(requesterRole);
        return userRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse getUser(String requesterRole, String userId) {
        requireLead(requesterRole);
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional
    public UserResponse updateUser(String requesterRole, String userId, UpdateUserRequest request) {
        requireLead(requesterRole);

        String normalizedEmail = normalizeOptional(request.getEmail());
        String normalizedPassword = normalizeOptional(request.getPassword());
        String normalizedRole = normalizeOptional(request.getRole());

        if (normalizedEmail == null && normalizedPassword == null && normalizedRole == null) {
            throw new BadRequestException("at least one field must be provided");
        }

        User user = findUserById(userId);

        if (normalizedEmail != null) {
            String email = normalizeEmail(normalizedEmail);
            validateEmailAvailableForUpdate(userId, email);
            user.setEmail(email);
        }

        if (normalizedPassword != null) {
            user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        }

        if (normalizedRole != null) {
            user.setRole(resolveRole(normalizedRole));
        }

        User saved = userRepository.save(user);
        log.info("User updated: {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(String requesterRole, String userId) {
        requireLead(requesterRole);
        User user = findUserById(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email already in use");
        }
    }

    private void validateEmailAvailableForUpdate(String userId, String email) {
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("email already in use");
                });
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User buildUser(String email, String rawPassword, String role) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .email(normalizeEmail(email))
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(resolveRole(role))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireLead(String requesterRole) {
        if (!"lead".equalsIgnoreCase(requesterRole)) {
            throw new UnauthorizedException("lead role required");
        }
    }

    private String resolveRole(String role) {
        if (role == null || role.isBlank()) {
            return "member";
        }

        return role.toLowerCase(Locale.ROOT);
    }
}
