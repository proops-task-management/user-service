package com.proops2026.userservice.service.impl;

import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.exception.UserAlreadyExistsException;
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
        validateEmailNotTaken(request.getEmail());
        User saved = userRepository.save(buildUser(request));
        log.info("User registered: {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new UserAlreadyExistsException("email already in use");
        }
    }

    private User buildUser(CreateUserRequest request) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("member")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
