package com.garageos.modules.jobcard.mapper;

import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.entity.JobCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface JobCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCardNumber", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "serviceDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobCard toEntity(CreateJobCardRequest request);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(expression =
            "java(jobCard.getCustomer().getFirstName() + " +
                    "(jobCard.getCustomer().getLastName()!=null ? " +
                    "\" \" + jobCard.getCustomer().getLastName() : \"\"))",
            target = "customerName")
    @Mapping(source = "customer.mobileNumber", target = "customerMobileNumber")

    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "vehicle.registrationNumber", target = "registrationNumber")
    @Mapping(source = "vehicle.brand", target = "brand")
    @Mapping(source = "vehicle.model", target = "model")
    JobCardResponse toResponse(JobCard jobCard);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCardNumber", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "serviceDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateJobCardRequest request,
                      @MappingTarget JobCard jobCard);
}