package com.garageos.modules.jobcard.event;

/**
 * Published (inside the closing transaction) when a Job Card moves to CLOSED.
 * Listeners that must not make closure fragile - e.g. final report generation -
 * react to it AFTER the commit, so a failure there can never undo or block the
 * closure.
 */
public record JobCardClosedEvent(Long jobCardId) {
}
