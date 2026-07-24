package com.garageos.modules.inspectionmaster.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.InspectionPriority;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "inspection_master_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionMasterItem extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "inspection_master_id",
            nullable = false
    )
    private InspectionMaster inspectionMaster;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String checkItem;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionPriority priority;

    @Builder.Default
    private Boolean mandatory = false;

    @Builder.Default
    private Integer displayOrder = 1;

    @Column(nullable = false, length = 255)
    private String serviceName;

    @Column(length = 1000)
    private String serviceDescription;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal labourCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal partCost = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    private BigDecimal estimatedLabourHours;
}