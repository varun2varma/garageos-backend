package com.garageos.modules.navigation.security;

import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Root-cause fix for a real authorization gap: WebSocketConfig previously
 * had no configureClientInboundChannel override at all, so any
 * authenticated user - any role, any garage, any customer - could STOMP
 * SUBSCRIBE directly to /topic/trips/{any tripId}/location and receive
 * that trip's live GPS stream. The equivalent REST read
 * (GET /api/v1/navigation/trips/{tripId}/location) was already
 * ownership-checked via DriverLocationServiceImpl.authorizeViewer; this
 * closes the same gap on the STOMP subscribe path using the identical
 * three-way rule, now centralized in NavigationTripAccessGuard so both
 * paths can never drift apart again.
 *
 * Deliberately scoped to SUBSCRIBE frames on exactly this topic shape -
 * CONNECT/SEND/DISCONNECT and any other destination pass through
 * unexamined, since location *publish* authorization is already handled
 * separately by LocationWebSocketController/DriverLocationServiceImpl.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripLocationTopicInterceptor implements ChannelInterceptor {

    private static final Pattern TRIP_LOCATION_TOPIC =
            Pattern.compile("^/topic/trips/(\\d+)/location$");

    private final NavigationTripAccessGuard accessGuard;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : TRIP_LOCATION_TOPIC.matcher(destination);

        if (matcher == null || !matcher.matches()) {
            // Not a per-trip location topic - nothing for this interceptor
            // to authorize.
            return message;
        }

        Long tripId = Long.valueOf(matcher.group(1));

        Principal user = accessor.getUser();

        if (!(user instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof GarageUserPrincipal principal)) {

            log.warn("Rejected unauthenticated SUBSCRIBE to {}", destination);

            throw new AccessDeniedException(
                    "Authentication required to subscribe to trip location updates."
            );
        }

        // Fail-closed: throws if this principal may not view this trip,
        // the same not-found-style rejection the REST endpoint uses, so
        // this cannot be used to discover that a trip id exists.
        accessGuard.authorizeViewer(principal, tripId);

        return message;
    }
}
