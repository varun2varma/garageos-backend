package com.garageos.core.enums.identity;

public enum PermissionCode {

    // Dashboard
    DASHBOARD_VIEW,

    // Customers
    CUSTOMER_VIEW,
    CUSTOMER_CREATE,
    CUSTOMER_UPDATE,
    CUSTOMER_DELETE,

    // Vehicles
    VEHICLE_VIEW,
    VEHICLE_CREATE,
    VEHICLE_UPDATE,
    VEHICLE_DELETE,

    // Job Cards
    JOB_CARD_VIEW,
    JOB_CARD_CREATE,
    JOB_CARD_UPDATE,
    JOB_CARD_CLOSE,

    // Inspection
    INSPECTION_VIEW,
    INSPECTION_CREATE,
    INSPECTION_UPDATE,
    INSPECTION_COMPLETE,

    // Estimates
    ESTIMATE_VIEW,
    ESTIMATE_CREATE,
    ESTIMATE_UPDATE,
    ESTIMATE_APPROVE,
    ESTIMATE_REJECT,

    // Repair
    REPAIR_VIEW,
    REPAIR_UPDATE,

    // Quality Check
    QUALITY_CHECK_VIEW,
    QUALITY_CHECK_UPDATE,

    // Invoice
    INVOICE_VIEW,
    INVOICE_GENERATE,

    // Payment
    PAYMENT_VIEW,
    PAYMENT_CREATE,

    // Inventory
    INVENTORY_VIEW,
    INVENTORY_CREATE,
    INVENTORY_UPDATE,
    INVENTORY_DELETE,

    // Users
    USER_VIEW,
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,

    // Roles
    ROLE_VIEW,
    ROLE_CREATE,
    ROLE_UPDATE,
    ROLE_DELETE,

    // Reports
    REPORT_VIEW,

    // Garage
    GARAGE_SETTINGS
}