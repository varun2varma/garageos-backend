package com.garageos.modules.invoiceitem.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.EstimateItemType;
import com.garageos.core.enums.catalog.PriceTaxMode;
import com.garageos.core.enums.catalog.TaxSource;
import com.garageos.modules.complaint.entity.Complaint;
import com.garageos.modules.invoice.entity.Invoice;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstimateItemType itemType;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // Immutable tax snapshot written when the invoice is issued (null on legacy lines).
    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @Column(length = 20)
    private String unit;

    @Column(name = "hsn_sac_code", length = 10)
    private String hsnSacCode;

    @Column(name = "tax_rule_code", length = 40)
    private String taxRuleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_tax_mode", length = 20)
    private PriceTaxMode priceTaxMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_source", length = 30)
    private TaxSource taxSource;

    @Column(name = "gst_rate_percent", precision = 5, scale = 2)
    private BigDecimal gstRatePercent;

    @Column(name = "taxable_value", precision = 12, scale = 2)
    private BigDecimal taxableValue;

    @Column(name = "cgst_amount", precision = 12, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", precision = 12, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", precision = 12, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "line_total", precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /** True when this line carries the canonical tax snapshot (false = legacy line). */
    public boolean hasTaxSnapshot() {
        return taxableValue != null && lineTotal != null;
    }
}