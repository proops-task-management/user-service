package com.proops2026.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private String id;
    private String email;
    private LocalDateTime createdAt;
}
