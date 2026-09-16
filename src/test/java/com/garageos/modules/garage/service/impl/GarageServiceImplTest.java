package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.enums.garage.WorkshopType;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.mapper.GarageMapper;
import com.garageos.modules.garage.repository.GarageRepository;
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

        verify(garageRepository, times(2)).save(any());

        Garage secondSaveArg = savedCaptor.getAllValues().get(1);
        assertThat(secondSaveArg.getGarageCode()).isEqualTo("G007");
        assertThat(secondSaveArg.getStatus()).isEqualTo(GarageStatus.ACTIVE);

        assertThat(response.getGarageCode()).isEqualTo("G007");
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
