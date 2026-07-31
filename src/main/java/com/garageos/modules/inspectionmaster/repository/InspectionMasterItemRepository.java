package com.garageos.modules.inspectionmaster.repository;

import com.garageos.modules.inspectionmaster.entity.InspectionMasterItem;
import com.garageos.modules.inspectionmaster.repository.projection.InspectionMasterItemKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InspectionMasterItemRepository
        extends JpaRepository<InspectionMasterItem, Long> {

    List<InspectionMasterItem> findByInspectionMasterIdOrderByDisplayOrder(
            Long inspectionMasterId
    );

    @Query("""
    SELECT
    i.inspectionMaster.id AS inspectionMasterId,
    i.checkItem AS checkItem
    FROM InspectionMasterItem i
    """)
    List<InspectionMasterItemKey> findAllKeys();

}