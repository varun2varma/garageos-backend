package com.garageos.modules.finalreport.dto.response;

import com.garageos.core.enums.report.FinalReportAudience;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the caller may do with the final report of one Job Card. Only reports
 * the caller is authorized to read are listed; {@code canViewOwnerReport} and
 * {@code canRegenerate} are always false for customers.
 */
@Getter
@Builder
public class FinalReportStatusResponse {

    private Long jobCardId;

    /** True once the Job Card is paid, delivered and closed - i.e. a report may exist. */
    private boolean ready;

    private boolean canViewCustomerReport;

    private boolean canViewOwnerReport;

    private boolean canRegenerate;

    /** Latest version of each report the caller may read. */
    private List<Item> reports;

    @Getter
    @Builder
    public static class Item {
        private FinalReportAudience audience;
        private int version;
        private String fileName;
        private long fileSize;
        private LocalDateTime generatedAt;
        /** Owner/manager callers only; null for customers. */
        private String emailStatus;
    }
}
