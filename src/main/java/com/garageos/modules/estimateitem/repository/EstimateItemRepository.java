package com.garageos.modules.estimateitem.repository;

import com.garageos.modules.estimateitem.entity.EstimateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstimateItemRepository
        extends JpaRepository<EstimateItem, Long> {

    List<EstimateItem> findByEstimateId(Long estimateId);

}