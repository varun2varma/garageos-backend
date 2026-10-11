package com.garageos.modules.navigation.controller;

import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import com.garageos.modules.navigation.service.NavigationTripMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The driver's pickup and delivery evidence, reached from the job card it belongs to
 * (booking -> pickup request -> trip -> media; the request carries the job card id once the job card is created).
 * The media rows are linked, not copied, so there is one cloud object per photo.
 */
@RestController
@RequestMapping("/api/v1/navigation/job-cards")
@RequiredArgsConstructor
public class JobCardTripEvidenceController {

    private final NavigationTripMediaService mediaService;

    @GetMapping("/{jobCardId}/evidence")
    public List<NavigationTripMediaResponse> evidence(@PathVariable Long jobCardId) {
        return mediaService.getJobCardEvidence(jobCardId);
    }
}
