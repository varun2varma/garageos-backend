package com.garageos.modules.catalog.service.impl;

import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.catalog.dto.request.ImportCatalogRequest;
import com.garageos.modules.catalog.dto.response.ImportReportResponse;
import com.garageos.modules.catalog.dto.response.ImportReportResponse.RowIssue;
import com.garageos.modules.catalog.entity.CatalogItem;
import com.garageos.modules.catalog.repository.CatalogItemRepository;
import com.garageos.modules.catalog.service.CatalogImportService;
import com.garageos.modules.catalog.service.CatalogService;
import com.garageos.modules.catalog.service.CatalogUnits;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogImportServiceImpl implements CatalogImportService {

    static final int MAX_ROWS = 500;

    private final CatalogItemRepository repository;
    private final CatalogService catalogService;
    private final GarageScope garageScope;

    /**
     * Deliberately NOT one transaction: each valid row is saved in its own short transaction so that a row the
     * database refuses (reported as invalid) can never roll back the other rows. Business-rule failures are
     * reported per row exactly as before. Dry run never calls save.
     */
    @Override
    public ImportReportResponse importCsv(ImportCatalogRequest request) {

        if (!garageScope.isUnrestricted(garageScope.principal())) {
            throw new ResourceNotFoundException("Not found.");
        }

        boolean dryRun = !Boolean.FALSE.equals(request.getDryRun());

        List<List<String>> rows = parseCsv(request.getCsv());

        if (rows.isEmpty()) {
            throw new BusinessException("The CSV is empty.");
        }

        List<String> header = rows.get(0).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();

        for (String required : List.of("type", "name", "default_unit")) {
            if (!header.contains(required)) {
                throw new BusinessException("The CSV header must contain: type, name, default_unit.");
            }
        }

        if (rows.size() - 1 > MAX_ROWS) {
            throw new BusinessException("An import is limited to " + MAX_ROWS + " rows.");
        }

        List<RowIssue> duplicates = new ArrayList<>();
        List<RowIssue> invalid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long created = 0;

        for (int i = 1; i < rows.size(); i++) {

            int rowNumber = i;
            List<String> cells = rows.get(i);
            String name = cell(cells, header, "name");

            try {
                CatalogItemType type = parseType(cell(cells, header, "type"));

                if (name.isBlank() || name.length() > 150) {
                    throw new BusinessException("Name is required and at most 150 characters.");
                }

                String normalized = CatalogService.normalize(name);
                String key = type + "|" + normalized;

                if (!seen.add(key)) {
                    duplicates.add(issue(rowNumber, name, "Repeated earlier in this file."));
                    continue;
                }

                if (repository.findByItemTypeAndNameNormalized(type, normalized).isPresent()) {
                    duplicates.add(issue(rowNumber, name, "Already in the catalog; not changed."));
                    continue;
                }

                String category = cell(cells, header, "category");
                if (category.length() > 60) {
                    throw new BusinessException("Category is at most 60 characters.");
                }

                String description = cell(cells, header, "description");
                if (description.length() > 500) {
                    throw new BusinessException("Description is at most 500 characters.");
                }

                CatalogItem item = new CatalogItem();
                item.setItemType(type);
                item.setName(name.trim());
                item.setNameNormalized(normalized);
                item.setDescription(description.isBlank() ? null : description);
                item.setCategory(category.isBlank() ? null : category);
                item.setDefaultUnit(CatalogUnits.normalize(cell(cells, header, "default_unit")));
                item.setCreatedByUserId(garageScope.principal().getId());

                // Never verified by an import: a code at most becomes PENDING_REVIEW.
                catalogService.applyClassification(
                        item, cell(cells, header, "hsn_sac_code"), cell(cells, header, "tax_rule_code"), false, null);

                if (!dryRun) {
                    try {
                        repository.save(item);
                    } catch (DataAccessException ex) {
                        invalid.add(issue(rowNumber, name, "Could not be saved; the row was skipped."));
                        continue;
                    }
                }

                created++;

            } catch (BusinessException ex) {
                invalid.add(issue(rowNumber, name, ex.getMessage()));
            }
        }

        return ImportReportResponse.builder()
                .dryRun(dryRun)
                .totalRows(rows.size() - 1)
                .created((int) created)
                .duplicates(duplicates)
                .invalid(invalid)
                .build();
    }

    private static RowIssue issue(int row, String name, String reason) {
        return RowIssue.builder().row(row).name(name).reason(reason).build();
    }

    private static String cell(List<String> cells, List<String> header, String column) {
        int index = header.indexOf(column);
        return index < 0 || index >= cells.size() ? "" : cells.get(index).trim();
    }

    private static CatalogItemType parseType(String raw) {
        try {
            return CatalogItemType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Type must be PART or SERVICE.");
        }
    }

    /** Small RFC 4180 reader: quoted fields, doubled quotes, CRLF or LF; blank lines are skipped. */
    static List<List<String>> parseCsv(String text) {

        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                addRow(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }

        row.add(field.toString());
        addRow(rows, row);

        return rows;
    }

    private static void addRow(List<List<String>> rows, List<String> row) {
        boolean blank = row.stream().allMatch(String::isBlank);
        if (!blank) {
            rows.add(row);
        }
    }
}
