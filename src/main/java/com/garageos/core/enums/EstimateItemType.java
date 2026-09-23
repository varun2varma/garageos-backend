package com.garageos.core.enums;

public enum EstimateItemType {

    LABOUR,

    PART,

    /**
     * Others/consumables (Mission's third estimate category). Like PART,
     * GST is not applied to this category - see
     * MoneyCalculator.calculateGST's doc comment.
     */
    OTHERS

}