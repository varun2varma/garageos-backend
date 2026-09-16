package com.garageos.modules.jobcard.repository;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.jobcard.entity.JobCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    boolean existsByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findByJobCardNumber(String jobCardNumber);

    Optional<JobCard> findTopByGarageIdOrderByIdDesc(Long garageId);

    long countByStatusNotIn(List<JobCardStatus> statuses);

    long countByStatus(JobCardStatus status);

    long countByServiceDate(LocalDate serviceDate);

    List<JobCard> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JobCard> findByGarage_IdIn(List<Long> garageIds, Pageable pageable);

    /**
     * Same garage-scoped listing, narrowed to a set of statuses - lets the
     * operational Job list ("Inspection", "Approval", "Repair", "QC",
     * "Invoice", "Delivery") filter server-side instead of pulling every
     * Job Card into the client and filtering there.
     */
    Page<JobCard> findByGarage_IdInAndStatusIn(
            List<Long> garageIds,
            List<JobCardStatus> statuses,
            Pageable pageable);

    List<JobCard> findByCustomer(Customer customer);

    long countByCustomer(Customer customer);

    boolean existsByBookingId(Long bookingId);

}