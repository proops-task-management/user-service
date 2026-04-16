package com.proops2026.userservice.service;

import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.request.CreateManagedUserRequest;
import com.proops2026.userservice.dto.request.UpdateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse register(CreateUserRequest request);

    List<UserResponse> listUsers(String requesterRole);

    UserResponse getUser(String requesterRole, String userId);

    UserResponse createManagedUser(String requesterRole, CreateManagedUserRequest request);

    UserResponse updateUser(String requesterRole, String userId, UpdateUserRequest request);

    void deleteUser(String requesterRole, String userId);
}
