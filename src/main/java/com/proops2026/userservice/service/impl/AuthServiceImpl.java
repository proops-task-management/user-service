package com.proops2026.userservice.service.impl;

import com.proops2026.userservice.dto.request.LoginRequest;
import com.proops2026.userservice.dto.response.LoginResponse;
import com.proops2026.userservice.exception.InvalidCredentialsException;
import com.proops2026.userservice.mapper.UserMapper;
import com.proops2026.userservice.model.User;
import com.proops2026.userservice.repository.UserRepository;
import com.proops2026.userservice.service.AuthService;
import com.proops2026.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = findAndValidateUser(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(user);
        log.info("User logged in: {}", request.getEmail());
        return buildLoginResponse(token, user);
    }

    private User findAndValidateUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("invalid email or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("invalid email or password");
        }
        return user;
    }

    private LoginResponse buildLoginResponse(String token, User user) {
        return LoginResponse.builder()
                .token(token)
                .user(userMapper.toUserInfo(user))
                .build();
    }
}
