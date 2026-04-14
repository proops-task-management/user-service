package com.proops2026.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "email and password are required")
    private String email;

    @NotBlank(message = "email and password are required")
    private String password;
}
