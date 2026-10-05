package com.garageos.core.enums.inventory;

public enum StockMovementType {

    /** Stock received. Quantity is positive. */
    STOCK_IN,

    /** Stock issued (optionally against a job card). Quantity is positive; applied as a negative delta. */
    STOCK_OUT,

    /** Stock-take correction. Signed, non-zero delta; a reason is mandatory. */
    ADJUSTMENT
}
