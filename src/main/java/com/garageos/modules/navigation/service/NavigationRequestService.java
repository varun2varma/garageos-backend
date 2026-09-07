package com.garageos.modules.navigation.service;

import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;

import java.util.List;

public interface NavigationRequestService {

    NavigationRequestResponse createRequest(
            Long customerId,
            CreateNavigationRequest request
    );

    NavigationRequestResponse getRequest(
            Long requestId
    );

    List<NavigationRequestResponse> getCustomerRequests(
            Long customerId
    );

    List<NavigationRequestResponse> getGarageRequests(
            Long garageId
    );
}