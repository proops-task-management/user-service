package com.proops2026.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String role;
    }
}
