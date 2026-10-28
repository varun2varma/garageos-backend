package com.garageos.modules.navigation.event;

/** Published inside the job-card creation transaction when the job card was linked to a booking's pickup request. */
public record TripEvidenceAdoptionRequested(Long jobCardId) {
}
