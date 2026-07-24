package com.garageos.modules.inspectionmaster.repository;

import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionMasterItemRepository
        extends JpaRepository<InspectionMasterItem, Long> {

    List<InspectionMasterItem> findByInspectionMasterIdOrderByDisplayOrder(
            Long inspectionMasterId
    );

}