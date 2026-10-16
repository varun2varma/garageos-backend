package com.garageos.core.security;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated user, read from the security context only - never from a request body. */
public final class CurrentActor {

    private CurrentActor() {
    }

    /** Id of the authenticated user, or null when there is none (system / scheduled work, unit tests). */
    public static Long userIdOrNull() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof GarageUserPrincipal p) {
            return p.getId();
        }

        return null;
    }
}
