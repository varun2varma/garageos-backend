package com.garageos.modules.identity.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.identity.dto.request.CreateUserRequest;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.dto.response.UserResponse;
import com.garageos.modules.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return ApiResponseUtil.created(
                "User created successfully.",
                service.createUser(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "User fetched successfully.",
                service.getUser(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        return ApiResponseUtil.success(
                "User updated successfully.",
                service.updateUser(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id) {

        service.deleteUser(id);

        return ApiResponseUtil.success(
                "User deleted successfully."
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String direction) {

        return ApiResponseUtil.success(
                "Users fetched successfully.",
                service.getAllUsers(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/technicians")
    public ResponseEntity<ApiResponse<List<UserResponse>>> technicians(){

        return ApiResponseUtil.success(

                "Technicians fetched successfully.",

                service.getTechnicians()

        );

    }

}