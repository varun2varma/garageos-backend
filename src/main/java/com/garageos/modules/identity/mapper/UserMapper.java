package com.garageos.modules.identity.mapper;

import com.garageos.modules.identity.dto.request.CreateUserRequest;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.dto.response.UserResponse;
import com.garageos.modules.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "firstLogin", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "authenticationProvider", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "roles", ignore = true)
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "garageId", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "firstLogin", ignore = true)
    @Mapping(target = "authenticationProvider", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateUserRequest request,
                      @MappingTarget User user);
}