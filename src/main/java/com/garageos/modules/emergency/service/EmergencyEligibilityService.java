package com.garageos.modules.emergency.service;

import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.entity.CustomerGarage;
import com.garageos.modules.customer.repository.CustomerGarageRepository;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.GarageOption;
import com.garageos.modules.emergency.dto.response.EmergencyResponses.GarageOptions;
import com.garageos.modules.garage.dto.response.NearbyGarageResponse;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.garage.service.GarageDiscoveryService;
import com.garageos.modules.garage.service.impl.GarageDiscoveryServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The ONE place that decides which garages a verified number may ask for roadside help at a given location. The
 * client never supplies eligibility; it only receives it.
 *
 * <ul>
 *   <li>The number belongs to a customer with a default garage: that garage is the only option. If it is inactive,
 *       outside the coverage radius or has no location, it is returned as unavailable with a reason. No other garage
 *       is offered in its place.</li>
 *   <li>No customer, or a customer without a default garage: the existing {@link GarageDiscoveryService} supplies the
 *       ACTIVE garages near the point (the same algorithm booking discovery uses; nothing is re-implemented).</li>
 * </ul>
 * This is only called for a number whose OTP has been verified, so telling the holder about their own default garage
 * discloses nothing to anyone else.
 */
@Service
public class EmergencyEligibilityService {

    private final CustomerRepository customerRepository;
    private final CustomerGarageRepository customerGarageRepository;
    private final GarageRepository garageRepository;
    private final GarageDiscoveryService discoveryService;
    private final double coverageRadiusKm;

    public EmergencyEligibilityService(
            CustomerRepository customerRepository,
            CustomerGarageRepository customerGarageRepository,
            GarageRepository garageRepository,
            GarageDiscoveryService discoveryService,
            @Value("${emergency.coverage-radius-km:40}") double coverageRadiusKm) {
        this.customerRepository = customerRepository;
        this.customerGarageRepository = customerGarageRepository;
        this.garageRepository = garageRepository;
        this.discoveryService = discoveryService;
        this.coverageRadiusKm = coverageRadiusKm;
    }

    @Transactional(readOnly = true)
    public GarageOptions eligibleFor(String verifiedMobile, BigDecimal latitude, BigDecimal longitude) {

        double lat = latitude == null ? Double.NaN : latitude.doubleValue();
        double lng = longitude == null ? Double.NaN : longitude.doubleValue();

        if (Double.isNaN(lat) || Double.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BusinessException("A valid location is required.");
        }

        Optional<Garage> defaultGarage = defaultGarageOf(verifiedMobile);

        if (defaultGarage.isPresent()) {
            return new GarageOptions(true, List.of(optionForDefault(defaultGarage.get(), lat, lng)));
        }

        List<GarageOption> nearby = discoveryService.findNearby(lat, lng, coverageRadiusKm).stream()
                .map(EmergencyEligibilityService::optionFromDiscovery)
                .toList();

        return new GarageOptions(false, nearby);
    }

    private Optional<Garage> defaultGarageOf(String mobile) {

        Optional<Customer> customer = customerRepository.findByMobileNumber(mobile);

        if (customer.isEmpty()) {
            return Optional.empty();
        }

        return customerGarageRepository.findByCustomerId(customer.get().getId()).stream()
                .filter(CustomerGarage::isDefaultGarage)
                .findFirst()
                .flatMap(row -> garageRepository.findById(row.getGarageId()));
    }

    private GarageOption optionForDefault(Garage g, double lat, double lng) {

        Double distance = hasLocation(g)
                ? round(GarageDiscoveryServiceImpl.haversineKm(lat, lng,
                        g.getLatitude().doubleValue(), g.getLongitude().doubleValue()))
                : null;

        String reason = null;

        if (g.getStatus() != GarageStatus.ACTIVE) {
            reason = "GARAGE_INACTIVE";
        } else if (distance == null) {
            reason = "NO_GARAGE_LOCATION";
        } else if (distance > coverageRadiusKm) {
            reason = "OUT_OF_COVERAGE";
        }

        return new GarageOption(g.getId(), g.getGarageName(), g.getCity(), distance, true, reason == null, reason);
    }

    private static GarageOption optionFromDiscovery(NearbyGarageResponse n) {
        return new GarageOption(n.getId(), n.getGarageName(), n.getCity(), n.getDistanceKm(), false, true, null);
    }

    private static boolean hasLocation(Garage g) {
        return g.getLatitude() != null && g.getLongitude() != null;
    }

    private static double round(double km) {
        return Math.round(km * 10.0) / 10.0;
    }
}
