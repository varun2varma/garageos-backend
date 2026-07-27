package com.garageos.core.api.response;

import com.garageos.core.util.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public final class ApiResponseUtil {

    private ApiResponseUtil() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.ok(response);
    }

    public static ResponseEntity<ApiResponse<Void>> success(String message) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static ResponseEntity<ApiResponse<Void>> badRequest(String message) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    public static ResponseEntity<ApiResponse<Void>> notFound(String message) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    public static ResponseEntity<ApiResponse<Void>> internalServerError(String message) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {

        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getRequestId())
                .build();

        return ResponseEntity.ok(response);
    }
}