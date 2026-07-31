package com.garageos.modules.customer.mapper;

import com.garageos.modules.customer.dto.response.portal.CustomerEstimateResponse;
import com.garageos.modules.customer.dto.response.portal.CustomerInvoiceResponse;
import com.garageos.modules.customer.dto.response.portal.CustomerJobCardResponse;
import com.garageos.modules.customer.dto.response.portal.CustomerProfileResponse;
import com.garageos.modules.customer.dto.response.portal.CustomerVehicleResponse;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.vehicle.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerPortalMapper {

    @Mapping(target = "name", expression = "java(customer.getFullName())")
    CustomerProfileResponse toProfile(Customer customer);

    CustomerVehicleResponse toVehicle(Vehicle vehicle);

    @Mapping(target = "registrationNumber",
            source = "vehicle.registrationNumber")
    CustomerJobCardResponse toJobCard(JobCard jobCard);

    @Mapping(target = "jobCardNumber",
            source = "jobCard.jobCardNumber")
    CustomerEstimateResponse toEstimate(Estimate estimate);

    @Mapping(target = "estimateNumber",
            source = "estimate.estimateNumber")
    CustomerInvoiceResponse toInvoice(Invoice invoice);

}