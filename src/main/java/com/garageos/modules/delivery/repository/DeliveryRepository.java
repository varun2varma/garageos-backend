package com.garageos.modules.delivery.repository;

import com.garageos.modules.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository
        extends JpaRepository<Delivery, Long> {

    boolean existsByJobCardId(Long jobCardId);

    boolean existsByInvoiceId(Long invoiceId);

}