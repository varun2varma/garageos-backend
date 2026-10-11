package com.garageos.modules.invoice.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.catalog.SupplyType;
import com.garageos.modules.estimate.entity.Estimate;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true)
    String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false, unique = true)
    Estimate estimate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InvoiceStatus invoiceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentStatus paymentStatus;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal gst;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal grandTotal;

    @Column(columnDefinition = "TEXT")
    String remarks;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    /**
     * Customer-payment-authorization investigation (V55): receivePayment()
     * already guards same-transaction double-payment via the PAID check,
     * but without row-level protection two concurrent requests could both
     * read PENDING before either commits. Standard JPA optimistic locking,
     * scoped to this entity only — not added to BaseEntity.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /** True when the canonical per-line tax snapshot below exists (false = legacy invoice). */
    @Column(name = "tax_snapshot", nullable = false)
    private boolean taxSnapshot = false;

    /** Total GST including the GST contained in tax-inclusive prices. [gst] keeps its legacy meaning. */
    @Column(name = "tax_total", precision = 12, scale = 2)
    private BigDecimal taxTotal;

    @Column(name = "cgst_total", precision = 12, scale = 2)
    private BigDecimal cgstTotal;

    @Column(name = "sgst_total", precision = 12, scale = 2)
    private BigDecimal sgstTotal;

    @Column(name = "igst_total", precision = 12, scale = 2)
    private BigDecimal igstTotal;

    /** Whole-rupee round-off already included in grandTotal. */
    @Column(name = "round_off", precision = 12, scale = 2)
    private BigDecimal roundOff;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_type", length = 20)
    private SupplyType supplyType;

    @Column(name = "supplier_gstin", length = 15)
    private String supplierGstin;

    @Column(name = "supplier_state", length = 100)
    private String supplierState;

    @Column(name = "place_of_supply_state", length = 100)
    private String placeOfSupplyState;
}