package com.garageos.modules.repairtask.service;

import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.repairtask.dto.request.AssignTechnicianRequest;
import com.garageos.modules.repairtask.dto.response.RepairTaskResponse;

import java.util.List;

public interface RepairTaskService {

    void createRepairTasks(Estimate estimate);

    RepairTaskResponse assignTechnician(
            Long repairTaskId,
            AssignTechnicianRequest request);

    RepairTaskResponse startRepair(Long repairTaskId);

    RepairTaskResponse completeRepair(Long repairTaskId);

    List<RepairTaskResponse> getRepairTasks(Long jobCardId);

    RepairTaskResponse getRepairTask(Long id);

}