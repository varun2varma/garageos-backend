package com.garageos.modules.garagemembership.service.impl;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garagemembership.dto.request.ApproveGarageMembershipRequest;
import com.garageos.modules.garagemembership.dto.request.JoinGarageRequest;
import com.garageos.modules.garagemembership.dto.response.GarageMembershipResponse;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.mapper.GarageMembershipMapper;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.garagemembership.service.GarageMembershipService;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GarageMembershipServiceImpl
        implements GarageMembershipService {

    private final GarageMembershipRepository membershipRepository;
    private final GarageRepository garageRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GarageMembershipMapper mapper;

    @Override
    public GarageMembershipResponse joinGarage(
            Long userId,
            JoinGarageRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found."));

        Garage garage = garageRepository
                .findByGarageCode(request.getGarageCode())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Garage not found."));

        if (membershipRepository.existsByGarage_IdAndUser_Id(
                garage.getId(),
                user.getId())) {

            throw new BusinessException(
                    "You have already requested to join this garage.");
        }

        GarageMembership membership =
                new GarageMembership();

        membership.setGarage(garage);

        membership.setUser(user);

        membership.setStatus(
                GarageMembershipStatus.PENDING);

        membership.setJoinedAt(LocalDateTime.now());

        membership =
                membershipRepository.save(membership);

        return buildResponse(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageMembershipResponse> getPendingMemberships(
            Long garageId) {

        return membershipRepository
                .findByGarage_IdAndStatus(
                        garageId,
                        GarageMembershipStatus.PENDING
                )
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageMembershipResponse> getEmployees(
            Long garageId) {

        return membershipRepository
                .findByGarage_IdAndStatus(
                        garageId,
                        GarageMembershipStatus.ACTIVE
                )
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    public GarageMembershipResponse approveMembership(
            Long membershipId,
            Long ownerId,
            ApproveGarageMembershipRequest request) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        if (membership.getStatus() !=
                GarageMembershipStatus.PENDING) {

            throw new BusinessException(
                    "Membership is already processed.");
        }

        User owner =
                userRepository.findById(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Owner not found."));

        User employee = membership.getUser();

        for (Long roleId : request.getRoleIds()) {

            Role role =
                    roleRepository.findById(roleId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Role not found."));

            if (!userRoleRepository.existsByUserIdAndRoleCode(
                    employee.getId(),
                    role.getCode())) {

                UserRole userRole =
                        UserRole.builder()
                                .user(employee)
                                .role(role)
                                .build();

                userRoleRepository.save(userRole);

            }

        }

        employee.setGarageId(
                membership.getGarage().getId());

        employee.setEmployeeCode(
                request.getEmployeeCode());

        userRepository.save(employee);

        membership.setStatus(
                GarageMembershipStatus.ACTIVE);

        membership.setApprovedAt(
                LocalDateTime.now());

        membership.setApprovedBy(owner);

        membership.setEmployeeCode(
                request.getEmployeeCode());

        membershipRepository.save(membership);

        return buildResponse(membership);
    }

    @Override
    public GarageMembershipResponse rejectMembership(
            Long membershipId,
            String remarks) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        if (membership.getStatus() !=
                GarageMembershipStatus.PENDING) {

            throw new BusinessException(
                    "Only pending membership can be rejected.");
        }

        membership.setStatus(
                GarageMembershipStatus.REJECTED);

        membership.setRemarks(remarks);

        membershipRepository.save(membership);

        return buildResponse(membership);
    }

    @Override
    public void removeMembership(Long membershipId) {

        GarageMembership membership =
                membershipRepository.findById(membershipId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Membership not found."));

        User user = membership.getUser();

        user.setGarageId(null);

        user.setEmployeeCode(null);

        userRepository.save(user);

        userRoleRepository.deleteByUserId(user.getId());

        membership.setStatus(
                GarageMembershipStatus.INACTIVE
        );

        membershipRepository.save(membership);

    }

    private GarageMembershipResponse buildResponse(
            GarageMembership membership) {

        System.out.println("========== MEMBERSHIP ==========");
        System.out.println("User Id      : " + membership.getUser().getId());
        System.out.println("First Name   : " + membership.getUser().getFirstName());
        System.out.println("Last Name    : " + membership.getUser().getLastName());
        System.out.println("Mobile       : " + membership.getUser().getMobile());
        System.out.println("Email        : " + membership.getUser().getEmail());
        System.out.println("================================");

        GarageMembershipResponse response =
                mapper.toResponse(membership);

        List<String> roles =
                userRoleRepository.findByUserId(
                                membership.getUser().getId())
                        .stream()
                        .map(userRole ->
                                userRole.getRole()
                                        .getCode()
                                        .name())
                        .filter(role -> !"USER".equals(role))
                        .sorted()
                        .toList();

        response.setRoles(roles);

        return response;
    }

}
