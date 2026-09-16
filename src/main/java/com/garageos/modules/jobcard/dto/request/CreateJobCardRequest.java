package com.garageos.modules.jobcard.dto.request;

import com.garageos.modules.complaint.dto.request.CreateComplaintRequest;
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

    private List<CreateComplaintRequest> complaints;
    private LocalDate estimatedDeliveryDate;

    private String remarks;

    /**
     * Optional - the confirmed Booking this Job Card originates from.
     * Every historical/booking-less Job Card creation leaves this null.
     * Never trusted blindly: JobCardServiceImpl.createJobCard validates
     * garage scope, booking status, and vehicle/customer consistency
     * before honoring it.
     */
    private Long bookingId;
}