package com.garageos.modules.dashboard.dto.response;

import com.garageos.core.enums.JobCardStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentJobResponse {

    Long jobCardId;

    String jobCardNumber;

    String customerName;

    String mobileNumber;

    String registrationNumber;

    String vehicleName;

    JobCardStatus status;

    LocalDate serviceDate;

    Long odometerReading;

    LocalDate estimatedDeliveryDate;

}