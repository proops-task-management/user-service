package com.proops2026.userservice.service;

import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;

public interface UserService {

    UserResponse register(CreateUserRequest request);
}
