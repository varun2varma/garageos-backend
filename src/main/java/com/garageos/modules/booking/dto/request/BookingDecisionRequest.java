package com.garageos.modules.booking.dto.request;

import lombok.Data;

/** Optional remarks the garage attaches when confirming or rejecting a booking. */
@Data
public class BookingDecisionRequest {

    private String remarks;
}
