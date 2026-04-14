package com.proops2026.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HealthResponse {

    private String status;
    private String service;
}
