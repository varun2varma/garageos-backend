package com.garageos.modules.qualitycheck.dto.response;

import com.garageos.core.enums.QualityCheckStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheckResponse {

    private Long id;

    private Long jobCardId;

    private String jobCardNumber;

    private QualityCheckStatus status;

    private String inspectedBy;

    private LocalDateTime inspectedAt;

    private String remarks;

}