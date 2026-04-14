package com.proops2026.userservice.service;

import com.proops2026.userservice.dto.request.LoginRequest;
import com.proops2026.userservice.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
