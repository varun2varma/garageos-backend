package com.garageos.modules.booking.repository;

import com.garageos.modules.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByGarageIdOrderByRequestedAtAsc(Long garageId);

    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    Optional<Booking> findByIdAndGarageId(Long id, Long garageId);
}
