package com.garageos.modules.garage.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.GarageCodeGenerator;
import com.garageos.modules.garage.dto.request.CreateGarageRequest;
import com.garageos.modules.garage.dto.response.GarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.modules.garage.mapper.GarageMapper;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageService;
import com.garageos.modules.identity.entity.Role;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.entity.UserRole;
import com.garageos.modules.identity.repository.RoleRepository;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GarageServiceImpl implements GarageService {

    private final GarageRepository garageRepository;
    private final GarageMapper garageMapper;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final GarageCodeGenerator garageCodeGenerator;

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

        garage = garageRepository.save(garage);

        garage.setGarageCode(
//                String.format("G%03d", garage.getId())
                garageCodeGenerator.generate()
        );

        garage = garageRepository.save(garage);

        user.setGarageId(garage.getId());
        user.setFirstLogin(false);

        userRepository.save(user);

        assignOwnerRole(user);

        return garageMapper.toResponse(garage);
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

}