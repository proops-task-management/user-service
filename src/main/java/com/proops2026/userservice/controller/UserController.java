package com.proops2026.userservice.controller;

import com.proops2026.userservice.dto.request.CreateUserRequest;
import com.proops2026.userservice.dto.request.CreateManagedUserRequest;
import com.proops2026.userservice.dto.request.CreateUserWithRoleRequest;
import com.proops2026.userservice.dto.request.UpdateUserRequest;
import com.proops2026.userservice.dto.response.UserResponse;
import com.proops2026.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/with-role")
    public ResponseEntity<UserResponse> registerWithRole(@Valid @RequestBody CreateUserWithRoleRequest request) {
        UserResponse response = userService.registerWithRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list(@RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(userService.listUsers(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(role, id));
    }

    @PostMapping("/admin")
    public ResponseEntity<UserResponse> createManagedUser(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateManagedUserRequest request) {
        UserResponse response = userService.createManagedUser(role, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(role, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id) {
        userService.deleteUser(role, id);
        return ResponseEntity.noContent().build();
    }
}
