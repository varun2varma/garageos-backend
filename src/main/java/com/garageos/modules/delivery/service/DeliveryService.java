package com.garageos.modules.delivery.service;

import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import org.springframework.data.domain.Page;

public interface DeliveryService {

    DeliveryResponse createDelivery(CreateDeliveryRequest request);

    /** Business completion of a delivery via its verified delivery trip (see NavigationTripServiceImpl.completeTrip). */
    DeliveryResponse completeDeliveryFromTrip(
            Long jobCardId,
            String deliveredBy,
            String receivedBy);

    DeliveryResponse getDelivery(Long id);

    Page<DeliveryResponse> getAllDeliveries(
            int page,
            int size,
            String sortBy,
            String direction);

    DeliveryResponse updateDelivery(
            Long id,
            CreateDeliveryRequest request);

    void deleteDelivery(Long id);

}