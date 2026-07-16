package com.garageos.modules.dashboard.controller;

import com.garageos.core.api.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {

        ApiResponse<Map<String, String>> response =
                ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("GarageOS is running successfully.")
                        .data(Map.of("status", "UP"))
                        .timestamp(LocalDateTime.now())
                        .requestId(UUID.randomUUID().toString())
                        .build();

        return ResponseEntity.ok(response);
    }

}