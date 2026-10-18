package com.garageos.modules.platform;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.garage.GarageStatus;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SUPER_ADMIN-only platform administration. Every route is guarded by {@code @PreAuthorize}; the only write is an audited garage status change. */
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService service;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlatformAdminService.Overview>> overview() {
        return ApiResponseUtil.success("Platform overview fetched.", service.overview());
    }

    @GetMapping("/garages/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlatformAdminService.GarageDetail>> garage(@PathVariable Long id) {
        return ApiResponseUtil.success("Garage fetched.", service.garage(id));
    }

    @Getter
    @Setter
    public static class ChangeStatusRequest {
        @NotNull
        private GarageStatus status;
        @NotBlank
        private String reason;
    }

    @PutMapping("/garages/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlatformAdminService.GarageDetail>> changeStatus(
            @AuthenticationPrincipal GarageUserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest body) {
        return ApiResponseUtil.success("Garage status updated.",
                service.changeStatus(id, body.getStatus(), body.getReason(), user == null ? null : user.getId()));
    }

    @GetMapping("/garages")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PlatformAdminService.GarageRow>>> garages(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) GarageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponseUtil.success("Garages fetched.", service.garages(q, status, page, size));
    }
}
