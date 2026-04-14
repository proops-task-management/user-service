package com.proops2026.userservice.controller;

import com.proops2026.userservice.dto.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(
                HealthResponse.builder()
                        .status("ok")
                        .service("user-service")
                        .build()
        );
    }
}
