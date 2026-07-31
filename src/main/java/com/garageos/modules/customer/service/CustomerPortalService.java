package com.garageos.modules.customer.service;

import com.garageos.modules.customer.dto.response.portal.*;

import java.util.List;

public interface CustomerPortalService {

    CustomerProfileResponse getProfile();

    CustomerDashboardResponse getDashboard();

    List<CustomerVehicleResponse> getVehicles();

    List<CustomerJobCardResponse> getJobCards();

    List<CustomerEstimateResponse> getEstimates();

    List<CustomerInvoiceResponse> getInvoices();

    CustomerRepairTrackingResponse trackRepair(String jobCardNumber);

}