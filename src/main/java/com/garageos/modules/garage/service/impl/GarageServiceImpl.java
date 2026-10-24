package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.modules.otp.service.OtpService;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.request.UpdateGarageLocationRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.dto.response.GarageSummaryResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.modules.garage.mapper.GarageMapper;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import com.garageos.core.exception.ConflictException;

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
    private final OtpService otpService;

    @Override
    @Transactional
    public GarageResponse createGarage(
            Long userId,
            CreateGarageRequest request) {

        // Row lock on the caller: registrations by one account run one at a time, so the duplicate check below is
        // race-free (the garage table has no unique key on name/address, and existing data may hold duplicates).
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        requireMayRegisterGarage(userId);

        // Before the OTP check so a duplicate never burns a one-time code.
        if (garageRepository.existsByOwnerUserIdAndGarageNameIgnoreCaseAndAddressIgnoreCaseAndCityIgnoreCase(
                userId, request.getGarageName(), request.getAddress(), request.getCity())) {
            throw new ConflictException(
                    "You have already registered a garage with this name, address and city.",
                    "GARAGE_ALREADY_REGISTERED");
        }

        if (otpService.isEnabled()) {
            // Always the authenticated user's own registered number, never client input.
            otpService.verifyAndConsume(
                    OtpPurpose.GARAGE_REGISTRATION, user.getMobile(), request.getOtp());
            // That OTP went to the user's own number and was just consumed: ownership is proven.
            user.markMobileVerified(java.time.LocalDateTime.now());
            userRepository.save(user);
        }

        Garage garage = garageMapper.toEntity(request);

        // Normalised, then structurally validated (shape, state code, check character, PAN match). "Format valid" is
        // not "verified with the GST portal"; nothing here claims the registration is real.
        garage.setGstNumber(validatedGstin(request.getGstNumber(), request.getPanNumber()));

        garage.setStatus(GarageStatus.ACTIVE);
        garage.setNextEmployeeSequence(1);
        // Corrective fix: Garage.ownerUserId (see its own doc comment) was
        // given an entity field but never actually populated here, so it
        // stayed permanently null exactly as before - the same bug that
        // comment described as already fixed. Never taken from the
        // request body: ownership is who is actually calling this
        // (authenticated userId), not client-suppliable data.
        garage.setOwnerUserId(userId);

        garage = garageRepository.save(garage);

        garage.setGarageCode(
                String.format("G%03d", garage.getId())
        );

        garage = garageRepository.save(garage);

        // The account's default garage is set only by the FIRST registration. An owner adding another garage keeps
        // their existing default: the new garage is reached through its ACTIVE membership (GarageScope), and
        // overwriting garageId would silently switch every request that does not name a garage to the new one.
        if (user.getGarageId() == null) {
            user.setGarageId(garage.getId());
        }
        user.setFirstLogin(false);

        userRepository.save(user);

        assignOwnerRole(user);

        createOwnerMembership(garage, user);

        return garageMapper.toResponse(garage);
    }

    /**
     * Registering a garage is open to an account with no staff role (the first registration, from a plain or
     * customer account) and to an OWNER (additional garages). An employee of another garage, or a platform
     * administrator, is refused: it would otherwise hand them an OWNER role and repoint their garage.
     */
    private void requireMayRegisterGarage(Long userId) {

        Set<RoleCode> roles = userRoleRepository.findByUserId(userId).stream()
                .map(r -> r.getRole().getCode())
                .collect(Collectors.toSet());

        if (roles.contains(RoleCode.OWNER)) {
            return;
        }

        boolean blocked = roles.stream().anyMatch(code -> code != RoleCode.USER && code != RoleCode.CUSTOMER);

        if (blocked) {
            throw new AccessDeniedException("This account cannot register a garage.");
        }
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
    public GarageSummaryResponse getGarage(GarageUserPrincipal principal, Long id) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        if (isGarageStaff(principal, garage)) {
            return garageMapper.toResponse(garage);
        }

        return garageMapper.toSummary(garage);
    }

    @Override
    public GarageResponse updateGarage(
            GarageUserPrincipal principal,
            Long id,
            CreateGarageRequest request) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        requireOwnerOf(principal, garage);

        garage.setGarageName(request.getGarageName());
        garage.setWorkshopType(request.getWorkshopType());
        garage.setNumberOfBays(request.getNumberOfBays());
        garage.setAddress(request.getAddress());
        garage.setLandmark(request.getLandmark());
        garage.setCity(request.getCity());
        garage.setState(request.getState());
        garage.setPincode(request.getPincode());
        // Only a CHANGED GSTIN is validated, so a garage that already holds an older value can still save other edits.
        String newGstin = com.garageos.core.util.GstinValidator.normalize(request.getGstNumber());
        if (!java.util.Objects.equals(newGstin, com.garageos.core.util.GstinValidator.normalize(garage.getGstNumber()))) {
            newGstin = validatedGstin(request.getGstNumber(), request.getPanNumber());
        }
        garage.setGstNumber(newGstin);
        garage.setPanNumber(request.getPanNumber());
        garage.setLatitude(request.getLatitude());
        garage.setLongitude(request.getLongitude());

        return garageMapper.toResponse(
                garageRepository.save(garage)
        );
    }

    /**
     * Owner-only add/edit of a garage's map location. Ownership is
     * enforced the same way every other garage-scoped mutation in this
     * codebase does it (InvoiceServiceImpl.authorizeInvoiceAction,
     * DeliveryServiceImpl, EstimateServiceImpl, etc.): the authenticated
     * principal's garageId must equal the target garage's id - never
     * trusted from the request body. Role (OWNER) is enforced separately
     * at the controller.
     */
    @Override
    public GarageResponse updateGarageLocation(
            GarageUserPrincipal principal,
            Long id,
            UpdateGarageLocationRequest request) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        // Same ownership rule as updateGarage: the owner of THIS garage, including an owner's further garages.
        requireOwnerOf(principal, garage);

        garage.setLatitude(request.getLatitude());
        garage.setLongitude(request.getLongitude());

        return garageMapper.toResponse(
                garageRepository.save(garage)
        );
    }

    /**
     * Only the OWNER of THIS garage may modify or delete it. The OWNER role
     * alone is not enough (an owner of Garage A must not touch Garage B):
     * the caller must also belong to, or be the registered owner of, the
     * target garage - never trusted from the request.
     */
    /** Off only as an emergency escape hatch (garagest.gstin.enforce-checksum=false); on by default. */
    @org.springframework.beans.factory.annotation.Value("${garagest.gstin.enforce-checksum:true}")
    private boolean enforceGstinChecksum = true;

    private String validatedGstin(String rawGstin, String rawPan) {

        String gstin = com.garageos.core.util.GstinValidator.normalize(rawGstin);

        if (gstin == null) {
            return null;
        }

        if (enforceGstinChecksum && !com.garageos.core.util.GstinValidator.isStructurallyValid(gstin)) {
            throw new BusinessException("The GSTIN is not valid. Check the 15 characters and try again.");
        }

        String pan = rawPan == null ? null : rawPan.trim().toUpperCase();

        if (enforceGstinChecksum && pan != null && !pan.isEmpty()
                && !pan.equals(com.garageos.core.util.GstinValidator.embeddedPan(gstin))) {
            throw new BusinessException("The GSTIN does not match the PAN entered.");
        }

        return gstin;
    }

    /** Member (any status) or registered owner of this garage. */
    private boolean isGarageStaff(GarageUserPrincipal principal, Garage garage) {

        if (principal == null) {
            return false;
        }

        if (principal.getGarageId() != null
                && principal.getGarageId().equals(garage.getId())) {
            return true;
        }

        if (principal.getId() == null) {
            return false;
        }

        return principal.getId().equals(garage.getOwnerUserId())
                || garageMembershipRepository.existsByGarage_IdAndUser_Id(
                garage.getId(), principal.getId());
    }

    private void requireOwnerOf(GarageUserPrincipal principal, Garage garage) {

        boolean owns =
                (principal.getGarageId() != null
                        && principal.getGarageId().equals(garage.getId()))
                        || (principal.getId() != null
                        && principal.getId().equals(garage.getOwnerUserId()));

        if (!owns) {
            throw new BusinessException("This garage does not belong to you.");
        }
    }

    @Override
    public void deleteGarage(GarageUserPrincipal principal, Long id) {

        Garage garage = garageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Garage not found."));

        requireOwnerOf(principal, garage);

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
    public List<GarageSummaryResponse> getAllGarages() {

        // ACTIVE garages only: a closed/inactive garage must never be offered
        // as a directory entry (join-garage, discovery).
        return garageRepository
                .findByStatus(GarageStatus.ACTIVE)
                .stream()
                .map(garageMapper::toSummary)
                .toList();

    }

}