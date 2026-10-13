package com.garageos.modules.delivery.preference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnPreferenceRepository extends JpaRepository<ReturnPreference, Long> {

    Optional<ReturnPreference> findByJobCardId(Long jobCardId);
}
