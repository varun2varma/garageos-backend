package com.garageos.core.enums.vehicle;

/**
 * Mission backlog #1 — "Customer RC needs to be verified." A vehicle
 * starts NOT_SUBMITTED; garage staff move it through
 * PENDING/VERIFIED/REJECTED (see VehicleController's rc-verification
 * endpoint, restricted to Manager/Service Advisor/Owner).
 */
public enum RcVerificationStatus {

    NOT_SUBMITTED,

    PENDING,

    VERIFIED,

    REJECTED
}
