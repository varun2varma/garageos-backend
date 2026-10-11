package com.garageos.modules.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ImportReportResponse {

    private boolean dryRun;
    private int totalRows;
    /** Rows saved (or that would be saved on a dry run). */
    private int created;
    private List<RowIssue> duplicates;
    private List<RowIssue> invalid;

    @Getter
    @Builder
    public static class RowIssue {
        /** 1-based data row number (the header is not counted). */
        private int row;
        private String name;
        private String reason;
    }
}
