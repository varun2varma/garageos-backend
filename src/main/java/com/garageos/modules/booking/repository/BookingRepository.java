package com.garageos.modules.booking.repository;

import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.modules.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByGarageIdOrderByRequestedAtAsc(Long garageId);

    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    Optional<Booking> findByIdAndGarageId(Long id, Long garageId);

    /**
     * Batched across a customer's whole vehicle list (Customer Live
     * Vehicle Journey, see CustomerVehicleJourneyServiceImpl) - one query
     * for every vehicle rather than one query per vehicle.
     */
    List<Booking> findByVehicleIdInAndStatusIn(
            List<Long> vehicleIds,
            List<BookingStatus> statuses
    );
}
