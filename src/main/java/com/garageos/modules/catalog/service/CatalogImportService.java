package com.garageos.modules.catalog.service;

import com.garageos.modules.catalog.dto.request.ImportCatalogRequest;
import com.garageos.modules.catalog.dto.response.ImportReportResponse;

/**
 * Platform-administrator import of catalog names from CSV.
 *
 * Template header: {@code type,name,category,default_unit,description} with optional
 * {@code hsn_sac_code,tax_rule_code}. An import can only ADD items. An existing item
 * (same type and normalised name) is reported as a duplicate and is never overwritten, so
 * an approved classification can never be changed by an import. A supplied code is stored
 * as PENDING_REVIEW at most: an import can never mark an item VERIFIED.
 */
public interface CatalogImportService {

    ImportReportResponse importCsv(ImportCatalogRequest request);
}
