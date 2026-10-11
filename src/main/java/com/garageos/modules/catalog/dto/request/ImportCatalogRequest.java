package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportCatalogRequest {

    /** CSV text with the header {@code type,name,category,default_unit,description[,hsn_sac_code,tax_rule_code]}. */
    @NotBlank
    @Size(max = 200_000)
    private String csv;

    /** Validate and report only; nothing is saved. Defaults to true so an import is never accidental. */
    private Boolean dryRun = Boolean.TRUE;
}
