package com.garageos.modules.customer.dto.response.portal;

import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerInvoiceDetailsResponse {

    private InvoiceResponse invoice;

    private String jobCardNumber;

    private List<EstimateItemResponse> items;

}