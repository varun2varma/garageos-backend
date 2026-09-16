package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.modules.garage.mapper.GarageMapper;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
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
public class GarageServiceImpl implements GarageService {

    private final GarageRepository garageRepository;
    private final GarageMapper garageMapper;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GarageMembershipRepository garageMembershipRepository;

    @Override
    @Transactional
    public GarageResponse createGarage(
            Long userId,
            CreateGarageRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        Garage garage = garageMapper.toEntity(request);

        garage.setStatus(GarageStatus.ACTIVE);
        garage.setNextEmployeeSequence(1);

        garage = garageRepository.save(garage);

        garage.setGarageCode(
                String.format("G%03d", garage.getId())
        );

        garage = garageRepository.save(garage);

        user.setGarageId(garage.getId());
        user.setFirstLogin(false);

        userRepository.save(user);

        assignOwnerRole(user);

        createOwnerMembership(garage, user);

        return garageMapper.toResponse(garage);
    }

    /**
     * Corrective fix: registering a garage set user.garageId and granted
     * the OWNER role, but never created a GarageMembership row. Every
     * garage-context consumer in the system resolves context from
     * GarageMembership - GET /garage-memberships/my, the dashboard's and
     * Job Card list's allGarages aggregation, and the
     * existsByGarage_IdAndUser_Id validation used whenever a garageId is
     * supplied - so a freshly registered owner had an empty membership
     * list and every one of those paths failed with
     * "No garage context for this account."
     *
     * The membership is created ACTIVE and pre-approved by the owner
     * themselves: an owner does not apply to their own garage and has
     * nobody to approve them.
     *
     * Roles are deliberately NOT written to garage_membership_role. That
     * table is dead - no code in this codebase has ever inserted into it,
     * and its NOT NULL `status` column has no counterpart on the entity,
     * so writing it would fail. GarageMembershipServiceImpl.buildResponse
     * derives a membership's roles from the global user_roles rows, which
     * assignOwnerRole above has already created. This mirrors exactly what
     * approveMembership does for an employee.
     *
     * Idempotent, so re-running against an owner who already has a
     * membership (e.g. the V40 backfill, or a retried request) cannot
     * produce a duplicate and violate uk_garage_membership.
     */
    private void createOwnerMembership(Garage garage, User user) {

        boolean alreadyMember =
                garageMembershipRepository.existsByGarage_IdAndUser_Id(
                        garage.getId(),
                        user.getId()
                );

        if (alreadyMember) {
            return;
        }

        GarageMembership membership = new GarageMembership();

        membership.setGarage(garage);
        membership.setUser(user);
        membership.setStatus(GarageMembershipStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setApprovedAt(LocalDateTime.now());
        membership.setApprovedBy(user);

        garageMembershipRepository.save(membership);

        // garage.owner_user_id has existed since V24 but nothing ever
        // populated it. Set it here so the column stops being permanently
        // null and the garage knows who owns it.
        garage.setOwnerUserId(user.getId());
        garageRepository.save(garage);
    }

    @Override
    @Transactional(readOnly = true)
    public GarageResponse getGarage(Long id) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        return garageMapper.toResponse(garage);
    }

    @Override
    public GarageResponse updateGarage(
            Long id,
            CreateGarageRequest request) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        garage.setGarageName(request.getGarageName());
        garage.setWorkshopType(request.getWorkshopType());
        garage.setNumberOfBays(request.getNumberOfBays());
        garage.setAddress(request.getAddress());
        garage.setLandmark(request.getLandmark());
        garage.setCity(request.getCity());
        garage.setState(request.getState());
        garage.setPincode(request.getPincode());
        garage.setGstNumber(request.getGstNumber());
        garage.setPanNumber(request.getPanNumber());

        return garageMapper.toResponse(
                garageRepository.save(garage)
        );
    }

    @Override
    public void deleteGarage(Long id) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        garageRepository.delete(garage);
    }

//    private void assignOwnerRole(User user) {
//
//        userRoleRepository.deleteByUserId(user.getId());
//
//        Role ownerRole = roleRepository.findByCode(RoleCode.OWNER)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("OWNER role not found."));
//
//        UserRole userRole = UserRole.builder()
//                .user(user)
//                .role(ownerRole)
//                .build();
//
//        userRoleRepository.save(userRole);
//    }

    private void assignOwnerRole(User user) {

        boolean alreadyAssigned =
                userRoleRepository.existsByUserIdAndRoleCode(
                        user.getId(),
                        RoleCode.OWNER
                );

        if (alreadyAssigned) {
            return;
        }

        Role ownerRole = roleRepository.findByCode(RoleCode.OWNER)
                .orElseThrow(() ->
                        new ResourceNotFoundException("OWNER role not found."));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(ownerRole)
                .build();

        userRoleRepository.save(userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GarageResponse> getAllGarages() {

        return garageRepository
                .findAll()
                .stream()
                .map(garageMapper::toResponse)
                .toList();

    }

}