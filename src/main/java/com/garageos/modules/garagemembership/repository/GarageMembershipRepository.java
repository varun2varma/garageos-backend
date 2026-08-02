package com.garageos.modules.garagemembership.repository;

import com.garageos.core.enums.garagemembership.GarageMembershipStatus;
import com.garageos.modules.garagemembership.entity.GarageMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarageMembershipRepository
        extends JpaRepository<GarageMembership, Long> {

    Optional<GarageMembership> findByGarage_IdAndUser_Id(
            Long garageId,
            Long userId
    );

    boolean existsByGarage_IdAndUser_Id(
            Long garageId,
            Long userId
    );

    List<GarageMembership> findByGarage_Id(
            Long garageId
    );

    List<GarageMembership> findByGarage_IdAndStatus(
            Long garageId,
            GarageMembershipStatus status
    );

    List<GarageMembership> findByUser_Id(
            Long userId
    );

}