package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.enums.garage.WorkshopType;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.mapper.GarageMapper;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import com.garageos.modules.garagemembership.repository.GarageMembershipRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the garageCode generate-then-persist sequence in
 * GarageServiceImpl.createGarage(): the code is only knowable after the
 * first insert assigns an id, so it is set on the entity and saved a
 * second time. That second save only actually reaches the database if
 * Garage.garageCode is not annotated updatable = false (the bug fixed
 * here) - a pure Mockito unit test cannot observe Hibernate's generated
 * SQL, so it verifies the service's save-order/value contract instead.
 * The actual DB-level persistence of garageCode was confirmed separately
 * against a live Postgres instance (POST /garages followed by
 * GET /garages/{id} returning the same non-null code).
 */
@ExtendWith(MockitoExtension.class)
class GarageServiceImplTest {

    @Mock private GarageRepository garageRepository;
    @Mock private GarageMapper garageMapper;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private GarageMembershipRepository garageMembershipRepository;

    @InjectMocks
    private GarageServiceImpl service;

    private CreateGarageRequest request() {
        CreateGarageRequest r = new CreateGarageRequest();
        r.setGarageName("Test Garage");
        r.setWorkshopType(WorkshopType.MULTI_BRAND);
        r.setNumberOfBays(2);
        r.setAddress("Addr");
        r.setCity("City");
        r.setState("State");
        r.setPincode("123456");
        return r;
    }

    @Test
    void createGarage_generatesCodeFollowingConvention_andSavesItOnSecondSave() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Garage inserted = new Garage();
        inserted.setId(7L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(garageMapper.toEntity(any())).thenReturn(inserted);
        when(userRoleRepository.existsByUserIdAndRoleCode(userId, RoleCode.OWNER)).thenReturn(true);

        ArgumentCaptor<Garage> savedCaptor = ArgumentCaptor.forClass(Garage.class);
        when(garageRepository.save(savedCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        when(garageMapper.toResponse(any())).thenAnswer(inv -> {
            Garage g = inv.getArgument(0);
            GarageResponse r = new GarageResponse();
            r.setId(g.getId());
            r.setGarageCode(g.getGarageCode());
            return r;
        });

        GarageResponse response = service.createGarage(userId, request());

        // Third save is the owner back-reference set alongside the new
        // owner membership; the code-generation contract is the first two.
        verify(garageRepository, atLeast(2)).save(any());

        Garage secondSaveArg = savedCaptor.getAllValues().get(1);
        assertThat(secondSaveArg.getGarageCode()).isEqualTo("G007");
        assertThat(secondSaveArg.getStatus()).isEqualTo(GarageStatus.ACTIVE);

        assertThat(response.getGarageCode()).isEqualTo("G007");
    }

    // ---- Owner garage context (Bug #1) ----

    /**
     * Registering a garage previously set users.garage_id and granted the
     * OWNER role but created no GarageMembership. Every garage-context
     * consumer resolves context from GarageMembership, so a freshly
     * registered owner had an empty membership list and every one of
     * those paths failed with "No garage context for this account."
     */
    @Test
    void createGarage_createsAnActiveOwnerMembership_soTheOwnerHasGarageContext() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Garage inserted = new Garage();
        inserted.setId(7L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(garageMapper.toEntity(any())).thenReturn(inserted);
        when(userRoleRepository.existsByUserIdAndRoleCode(userId, RoleCode.OWNER)).thenReturn(true);
        when(garageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(7L, userId)).thenReturn(false);
        when(garageMembershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGarage(userId, request());

        ArgumentCaptor<GarageMembership> captor =
                ArgumentCaptor.forClass(GarageMembership.class);
        verify(garageMembershipRepository).save(captor.capture());

        GarageMembership membership = captor.getValue();
        assertThat(membership.getUser().getId()).isEqualTo(userId);
        assertThat(membership.getGarage().getId()).isEqualTo(7L);

        // ACTIVE, not PENDING: an owner does not apply to their own garage
        // and has nobody to approve them.
        assertThat(membership.getStatus()).isEqualTo(GarageMembershipStatus.ACTIVE);
        assertThat(membership.getApprovedAt()).isNotNull();
        assertThat(membership.getApprovedBy()).isEqualTo(user);
    }

    @Test
    void createGarage_setsTheGarageOwnerBackReference() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Garage inserted = new Garage();
        inserted.setId(7L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(garageMapper.toEntity(any())).thenReturn(inserted);
        when(userRoleRepository.existsByUserIdAndRoleCode(userId, RoleCode.OWNER)).thenReturn(true);
        when(garageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(7L, userId)).thenReturn(false);
        when(garageMembershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGarage(userId, request());

        // owner_user_id has existed since V24 but had no entity field, so
        // nothing could ever populate it.
        assertThat(inserted.getOwnerUserId()).isEqualTo(userId);
    }

    /**
     * uk_garage_membership(garage_id, user_id) is unique, so a second
     * membership for the same owner would be a constraint violation.
     */
    @Test
    void createGarage_doesNotDuplicateAnExistingOwnerMembership() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Garage inserted = new Garage();
        inserted.setId(7L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(garageMapper.toEntity(any())).thenReturn(inserted);
        when(userRoleRepository.existsByUserIdAndRoleCode(userId, RoleCode.OWNER)).thenReturn(true);
        when(garageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(garageMembershipRepository.existsByGarage_IdAndUser_Id(7L, userId)).thenReturn(true);

        service.createGarage(userId, request());

        verify(garageMembershipRepository, never()).save(any());
    }

    @Test
    void updateGarage_doesNotModifyGarageCode() {

        Garage existing = new Garage();
        existing.setId(9L);
        existing.setGarageCode("G009");

        when(garageRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(garageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(garageMapper.toResponse(any())).thenAnswer(inv -> {
            Garage g = inv.getArgument(0);
            GarageResponse r = new GarageResponse();
            r.setId(g.getId());
            r.setGarageCode(g.getGarageCode());
            return r;
        });

        GarageResponse response = service.updateGarage(9L, request());

        assertThat(response.getGarageCode()).isEqualTo("G009");
    }
}
