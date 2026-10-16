package com.garageos.core.util;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.modules.jobcard.entity.JobCard;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who is shown as the responsible staff member on documents ("Service Advisor" field).
 *
 * The rule uses ONLY what was persisted when the job card was created (creator id, name and the roles the creator
 * held at that moment):
 * <ul>
 *   <li>creator held SERVICE_ADVISOR: that person is shown as the Service Advisor;</li>
 *   <li>otherwise a creator who held MANAGER / OWNER: that person is shown as the responsible Manager / Owner;</li>
 *   <li>otherwise (historical job cards, or no reliable data): a neutral label. No name is ever guessed from a
 *       current role, an arbitrary employee or a hardcoded string.</li>
 * </ul>
 */
public final class StaffAttribution {

    public static final String NOT_RECORDED = "Garage team (not individually recorded)";

    private StaffAttribution() {
    }

    public static String serviceAdvisorLabel(JobCard jobCard) {

        if (jobCard == null || jobCard.getCreatedByUserId() == null
                || jobCard.getCreatedByName() == null || jobCard.getCreatedByName().isBlank()) {
            return NOT_RECORDED;
        }

        Set<String> roles = jobCard.getCreatedByRoles() == null || jobCard.getCreatedByRoles().isBlank()
                ? Set.of()
                : Arrays.stream(jobCard.getCreatedByRoles().split(",")).map(String::trim).collect(Collectors.toSet());

        String name = jobCard.getCreatedByName().trim();

        if (roles.contains(RoleCode.SERVICE_ADVISOR.name())) {
            return name;
        }

        if (roles.contains(RoleCode.MANAGER.name())) {
            return name + " (Manager)";
        }

        if (roles.contains(RoleCode.OWNER.name())) {
            return name + " (Owner)";
        }

        return NOT_RECORDED;
    }
}
