package com.garageos.modules.customer.dto.response.portal;

import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerEstimateDetailsResponse {

    private EstimateResponse estimate;

    private List<EstimateItemResponse> items;

}