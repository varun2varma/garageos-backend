package com.garageos.core.enums.catalog;

/** Where a line GST rate came from. CATALOG_RULE: effective-dated rule of a classified catalog item. CONFIGURED_FALLBACK: interim configured default for an unclassified line. NONE: line carries no GST. */
public enum TaxSource {
    CATALOG_RULE,
    CONFIGURED_FALLBACK,
    NONE
}
