package com.garageos.modules.repairtask.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRepairTaskRequest {

    @NotNull
    private Long estimateItemId;

}