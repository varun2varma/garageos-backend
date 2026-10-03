package com.garageos.modules.garage.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Full garage record, including the business identifiers (PAN, GSTIN).
 * Returned only in an authorized garage context: to the garage owner/staff
 * for their own garage, and from create/update/location endpoints. Every
 * other caller gets {@link GarageSummaryResponse}.
 */
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GarageResponse extends GarageSummaryResponse {

    String gstNumber;

    String panNumber;

}
