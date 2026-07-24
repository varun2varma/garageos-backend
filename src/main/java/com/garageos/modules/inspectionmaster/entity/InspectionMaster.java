package com.garageos.modules.inspectionmaster.entity;

import com.garageos.core.audit.BaseEntity;
import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionMaster extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    private String variant;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    private TransmissionType transmissionType;

    private Integer minYear;

    private Integer maxYear;

    @Column(nullable = false)
    private Integer minOdometer;

    @Column(nullable = false)
    private Integer maxOdometer;

    @Builder.Default
    private Boolean active = true;

    @OneToMany(
            mappedBy = "inspectionMaster",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<InspectionMasterItem> items = new ArrayList<>();

}