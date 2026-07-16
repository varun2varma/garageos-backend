package com.garageos.modules.delivery.service;

import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import org.springframework.data.domain.Page;

public interface DeliveryService {

    DeliveryResponse createDelivery(CreateDeliveryRequest request);

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