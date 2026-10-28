package com.garageos.modules.navigation.storage;

import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decides where new trip evidence is stored, from server-side records only: the garage code of the trip's request and,
 * when that request is already linked to a job card, the job-card number. Without a job card the evidence goes to the
 * provisional {@code TRIP-<tripId>} owner and is adopted into the job-card folder once the job card exists
 * ({@code TripEvidenceAdoptionService}). An empty result means "use the previous layout".
 */
@Component
@RequiredArgsConstructor
public class TripEvidenceFolderResolver {

    private final NavigationRequestRepository requestRepository;
    private final GarageRepository garageRepository;
    private final JobCardRepository jobCardRepository;

    /** Where a trip photo goes: authoritative garage code, the job card when linked (else null), and the folder. */
    public record Placement(String garageCode, Long jobCardId, String jobCardNumber, String folder) {
    }

    public Optional<String> folderFor(NavigationTrip trip, TripMediaStage stage) {
        return placementFor(trip, stage).map(Placement::folder);
    }

    public Optional<Placement> placementFor(NavigationTrip trip, TripMediaStage stage) {

        if (trip == null || trip.getNavigationRequestId() == null || stage == null) {
            return Optional.empty();
        }

        NavigationRequest request = requestRepository.findById(trip.getNavigationRequestId()).orElse(null);

        if (request == null || request.getGarageId() == null) {
            return Optional.empty();
        }

        String garageCode = garageRepository.findById(request.getGarageId()).map(Garage::getGarageCode).orElse(null);

        String owner = TripEvidenceKeys.provisionalOwner(trip.getId());
        Long linkedJobCardId = null;
        String linkedNumber = null;

        if (request.getJobCardId() != null) {
            String number = jobCardRepository.findById(request.getJobCardId()).map(JobCard::getJobCardNumber).orElse(null);
            // the number embeds the garage code; anything else is not this garage's job card
            if (number != null && garageCode != null && number.startsWith(garageCode + "-")) {
                owner = number;
                linkedJobCardId = request.getJobCardId();
                linkedNumber = number;
            }
        }

        String folder = TripEvidenceKeys.folder(garageCode, owner, stage);

        return folder == null ? Optional.empty() : Optional.of(new Placement(garageCode, linkedJobCardId, linkedNumber, folder));
    }
}
