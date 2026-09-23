package com.garageos.core.enums;

/**
 * Mechanic work priority (Mission backlog #20) — belongs to RepairTask,
 * never to JobCard's own global status, per the mission's own explicit
 * instruction: "Priority belongs to RepairTask, not JobCard global
 * status." Settable only by Service Advisor/Manager/Owner (see
 * RepairTaskController.PRIORITY_ROLES) — a technician cannot change the
 * priority of their own work.
 */
public enum RepairTaskPriority {

    LOW,

    NORMAL,

    HIGH,

    URGENT
}
