package com.garageos.core.util;

import com.garageos.modules.garage.repository.GarageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GarageCodeGenerator {

    private final GarageRepository garageRepository;

    public String generate() {

        long count = garageRepository.count() + 1;

        return String.format("G%03d", count);

    }

}