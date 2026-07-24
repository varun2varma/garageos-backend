package com.garageos.modules.invoiceitem.repository;

import com.garageos.modules.invoiceitem.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository
        extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

}