package com.garageos.modules.inspectionmaster.service.impl;

import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.inspectionmaster.dto.request.CreateInspectionMasterItemRequest;
import com.garageos.modules.inspectionmaster.dto.response.InspectionMasterItemResponse;
import com.garageos.modules.inspectionmaster.entity.InspectionMaster;
import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.mapper.InspectionMasterItemMapper;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterItemRepository;
import com.garageos.modules.inspectionmaster.repository.InspectionMasterRepository;
import com.garageos.modules.inspectionmaster.service.InspectionMasterItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionMasterItemServiceImpl
        implements InspectionMasterItemService {

    private final InspectionMasterRepository inspectionMasterRepository;

    private final InspectionMasterItemRepository repository;

    private final InspectionMasterItemMapper mapper;

    @Override
    public InspectionMasterItemResponse createInspectionMasterItem(
            Long inspectionMasterId,
            CreateInspectionMasterItemRequest request) {

        InspectionMaster master = inspectionMasterRepository.findById(inspectionMasterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection Master not found with id : " + inspectionMasterId));

        InspectionMasterItem entity = mapper.toEntity(request);

        entity.setInspectionMaster(master);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public InspectionMasterItemResponse getInspectionMasterItem(Long id) {

        InspectionMasterItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection Master Item not found with id : " + id));

        return mapper.toResponse(entity);
    }

    @Override
    public List<InspectionMasterItemResponse> getInspectionMasterItems(
            Long inspectionMasterId) {

        return mapper.toResponseList(
                repository.findByInspectionMasterIdOrderByDisplayOrder(
                        inspectionMasterId));
    }

    @Override
    public InspectionMasterItemResponse updateInspectionMasterItem(
            Long id,
            CreateInspectionMasterItemRequest request) {

        InspectionMasterItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection Master Item not found with id : " + id));

        mapper.updateEntity(request, entity);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public void deleteInspectionMasterItem(Long id) {

        InspectionMasterItem entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inspection Master Item not found with id : " + id));

        repository.delete(entity);
    }

}