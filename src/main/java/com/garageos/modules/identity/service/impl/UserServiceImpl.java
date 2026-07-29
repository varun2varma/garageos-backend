package com.garageos.modules.identity.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.dto.request.CreateUserRequest;
import com.garageos.modules.identity.dto.request.UpdateUserRequest;
import com.garageos.modules.identity.dto.response.UserResponse;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.mapper.UserMapper;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import com.garageos.modules.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserMapper mapper;

    private final PasswordEncoder passwordEncoder;

    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    "Username already exists : " + request.getUsername());
        }

        User user = mapper.toEntity(request);

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        user.setStatus(UserStatus.ACTIVE);

        user.setFirstLogin(true);

        user = userRepository.save(user);

        /*
         * Assign Roles
         */
        if (request.getRoleIds() != null &&
                !request.getRoleIds().isEmpty()) {

            List<Role> roles =
                    roleRepository.findAllById(request.getRoleIds());

            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessException("One or more roles are invalid.");
            }

            User finalUser = user;
            List<UserRole> userRoles = roles.stream()

                    .map(role -> UserRole.builder()

                            .user(finalUser)

                            .role(role)

                            .build())

                    .toList();

            userRoleRepository.saveAll(userRoles);

            user.setUserRoles(userRoles);
        }

        return buildResponse(user);
    }

    private UserResponse buildResponse(User user) {

        return UserResponse.builder()

                .id(user.getId())

                .garageId(user.getGarageId())

                .username(user.getUsername())

                .firstName(user.getFirstName())

                .lastName(user.getLastName())

                .email(user.getEmail())

                .mobile(user.getMobile())

                .status(user.getStatus().name())

                .firstLogin(user.getFirstLogin())

                .roles(

                        user.getUserRoles()

                                .stream()

                                .map(userRole ->
                                        userRole
                                                .getRole()
                                                .getCode()
                                                .name())

                                .sorted()

                                .toList()

                )

                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : " + id));

        return buildResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : " + id));

        userRepository.delete(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(int page,
                                          int size,
                                          String sortBy,
                                          String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable =
                PageRequest.of(page, size, sort);

        Page<User> users =
                userRepository.findAll(pageable);

        return users.map(this::buildResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found : " + id));

        mapper.updateEntity(request, user);

        user.getUserRoles().clear();

        if (request.getRoleIds() != null) {

            List<Role> roles =
                    roleRepository.findAllById(request.getRoleIds());

            if (roles.size() != request.getRoleIds().size()) {
                throw new BusinessException("One or more roles are invalid.");
            }

            User finalUser = user;
            List<UserRole> userRoles =
                    roles.stream()

                            .map(role -> UserRole.builder()

                                    .user(finalUser)

                                    .role(role)

                                    .build())

                            .toList();

            user.getUserRoles().addAll(userRoles);
        }

        user = userRepository.save(user);

        return buildResponse(user);
    }


}
