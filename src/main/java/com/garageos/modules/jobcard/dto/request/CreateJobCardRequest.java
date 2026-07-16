package com.garageos.modules.jobcard.dto.request;

import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateJobCardRequest {

    @NotNull(message = "Vehicle id is required.")
    private Long vehicleId;

    @NotNull(message = "Odometer reading is required.")
    private Long odometerReading;

    private List<ComplaintResponse> complaints;
    private LocalDate estimatedDeliveryDate;

    private String remarks;
}