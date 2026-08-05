package com.garageos.modules.identity.service;

import com.garageos.modules.identity.dto.request.CreateUserRequest;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.dto.response.UserResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(Long id);

    UserResponse updateUser(Long id,
                            UpdateUserRequest request);

    void deleteUser(Long id);

    Page<UserResponse> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction
    );

    List<UserResponse> getTechnicians();

}