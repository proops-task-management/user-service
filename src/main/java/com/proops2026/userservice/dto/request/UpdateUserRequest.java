package com.proops2026.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Email(message = "email format is invalid")
    private String email;

    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @Pattern(regexp = "^(?i)(member|lead)$", message = "role must be one of: member, lead")
    private String role;
}
