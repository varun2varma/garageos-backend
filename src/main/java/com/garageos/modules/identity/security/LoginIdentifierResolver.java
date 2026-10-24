package com.garageos.modules.identity.security;

import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The single server-side policy that turns what a person typed into the login box (username, email or mobile) into
 * exactly one account, BEFORE the password is checked.
 *
 * <ul>
 *   <li>Username: exact match (existing accounts keep working unchanged).</li>
 *   <li>Email: case-insensitive. Stored values are never rewritten; only the comparison ignores case.</li>
 *   <li>Mobile: spaces, dashes, dots and brackets are ignored; a leading {@code +91}, {@code 91} or {@code 0} is
 *       stripped to the stored 10-digit Indian form; any other {@code +<digits>} number is matched as typed.</li>
 *   <li>If the input matches more than one distinct account (for example a username equal to another user's email
 *       or mobile), nothing is resolved. The caller treats that exactly like an unknown account, so the response
 *       never reveals that the identifier exists or collides.</li>
 * </ul>
 */
@Component
public class LoginIdentifierResolver {

    private static final Logger log = LoggerFactory.getLogger(LoginIdentifierResolver.class);

    private static final Pattern PHONE_SHAPE = Pattern.compile("^\\+?\\d{7,15}$");
    private static final Pattern INDIAN_MOBILE = Pattern.compile("^[6-9]\\d{9}$");

    private final UserRepository userRepository;

    public LoginIdentifierResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The one account the identifier unambiguously names, or empty (unknown, blank or ambiguous). */
    @Transactional(readOnly = true)
    public Optional<User> resolve(String raw) {

        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String identifier = raw.trim();

        Map<Long, User> matches = new LinkedHashMap<>();

        userRepository.findByUsername(identifier).ifPresent(u -> matches.put(u.getId(), u));

        if (identifier.indexOf('@') > 0) {
            List<User> byEmail = userRepository.findAllByEmailIgnoreCase(identifier);
            User chosen = byEmail.size() <= 1
                    ? (byEmail.isEmpty() ? null : byEmail.get(0))
                    // Legacy rows that differ only by case: the exact spelling the user typed is the tie-break.
                    : exactEmailMatch(byEmail, identifier);
            if (chosen != null) {
                matches.put(chosen.getId(), chosen);
            } else if (byEmail.size() > 1) {
                byEmail.forEach(u -> matches.put(u.getId(), u));
            }
        }

        Set<String> mobiles = mobileCandidates(identifier);
        if (!mobiles.isEmpty()) {
            userRepository.findAllByMobileIn(mobiles).forEach(u -> matches.put(u.getId(), u));
        }

        if (matches.size() > 1) {
            // Counts only: the identifier itself is personal data and is never logged.
            log.warn("[AUTH] Ambiguous login identifier rejected. matchedAccounts={}", matches.size());
            return Optional.empty();
        }

        return matches.values().stream().findFirst();
    }

    private static User exactEmailMatch(List<User> candidates, String typed) {
        List<User> exact = candidates.stream().filter(u -> typed.equals(u.getEmail())).toList();
        return exact.size() == 1 ? exact.get(0) : null;
    }

    /**
     * Every stored spelling the typed text could mean as a phone number (empty when it does not look like one).
     * Package-visible for unit tests.
     */
    static Set<String> mobileCandidates(String identifier) {

        String cleaned = identifier.replaceAll("[\\s\\-().]", "");

        Set<String> out = new LinkedHashSet<>();

        if (!PHONE_SHAPE.matcher(cleaned).matches()) {
            return out;
        }

        out.add(identifier);
        out.add(cleaned);

        String digits = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned;

        if (digits.length() == 12 && digits.startsWith("91")) {
            addIfIndian(out, digits.substring(2));
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            addIfIndian(out, digits.substring(1));
        } else if (digits.length() == 10) {
            addIfIndian(out, digits);
        }

        return out;
    }

    private static void addIfIndian(Set<String> out, String tenDigits) {
        if (INDIAN_MOBILE.matcher(tenDigits).matches()) {
            out.add(tenDigits);
        }
    }
}
