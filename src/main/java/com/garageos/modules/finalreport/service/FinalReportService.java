package com.garageos.modules.finalreport.service;

import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.modules.finalreport.dto.response.FinalReportStatusResponse;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;

public interface FinalReportService {

    /** What the caller can see / do for this Job Card's final report. */
    FinalReportStatusResponse getStatus(GarageUserPrincipal principal, Long jobCardId);

    /**
     * The PDF of one audience's report (latest version unless {@code version}
     * is given). Audience access is decided server-side from the caller's
     * relationship to the Job Card - never from the request.
     */
    FinalReportFile download(GarageUserPrincipal principal, Long jobCardId,
                             FinalReportAudience audience, Integer version);

    /** OWNER / MANAGER of the Job Card's garage: issues the next version of both reports (or v1 if none exist). */
    FinalReportStatusResponse regenerate(GarageUserPrincipal principal, Long jobCardId);

    /**
     * System action after Job Card closure: issues v1 of both reports if they
     * do not exist yet and e-mails them. Never throws for e-mail problems.
     */
    void generateOnClosure(Long jobCardId);

    record FinalReportFile(byte[] content, String fileName, String contentType, int version) {
    }
}
