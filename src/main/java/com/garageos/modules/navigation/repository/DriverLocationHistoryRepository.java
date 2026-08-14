package com.garageos.modules.navigation.repository;

import com.garageos.modules.navigation.entity.DriverLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverLocationHistoryRepository
        extends JpaRepository<DriverLocationHistory, Long> {
}