package com.garageos.modules.repairtask.dto.request;

import com.garageos.core.enums.RepairTaskPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetPriorityRequest {

    @NotNull(message = "Priority is required.")
    private RepairTaskPriority priority;
}
