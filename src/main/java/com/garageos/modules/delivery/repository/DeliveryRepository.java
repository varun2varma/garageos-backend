package com.garageos.modules.delivery.repository;

import com.garageos.modules.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    boolean existsByJobCardId(Long jobCardId);

    boolean existsByInvoiceId(Long invoiceId);

    Optional<Delivery> findByJobCardId(Long jobCardId);

}