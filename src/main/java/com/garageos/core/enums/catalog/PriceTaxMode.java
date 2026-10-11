package com.garageos.core.enums.catalog;

import com.garageos.core.enums.EstimateItemType;

/** How an entered price relates to GST: INCLUSIVE (tax is inside the price), EXCLUSIVE (tax is added on top), NO_TAX. */
public enum PriceTaxMode {
    INCLUSIVE,
    EXCLUSIVE,
    NO_TAX;

    /** Existing business meaning of entered prices: PART inclusive, LABOUR exclusive, OTHERS untaxed. */
    public static PriceTaxMode forType(EstimateItemType type) {
        if (type == null) {
            return NO_TAX;
        }
        return switch (type) {
            case PART -> INCLUSIVE;
            case LABOUR -> EXCLUSIVE;
            default -> NO_TAX;
        };
    }
}
