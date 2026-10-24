package com.garageos.core.ratelimit;

import com.garageos.modules.identity.security.ClientIpResolver;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Per-request limits for the endpoints that are cheap to abuse and that carry no identifier in a form a filter can
 * read without consuming the body: login, registration, OTP send, place search, garage discovery, booking creation
 * and evidence uploads. Identifier- and phone-based limits (login per account, OTP per number, emergency per number)
 * are applied by the owning service through {@link RateLimitService#require}.
 *
 * <p>Runs after the JWT filter, so an authenticated caller is limited per user (a garage's staff behind one NAT
 * address do not share one budget); an anonymous caller is limited per client IP. The IP comes from
 * {@link ClientIpResolver}, which believes {@code X-Forwarded-For} only when the TCP peer is a private/proxy address.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private enum Subject { IP, USER_OR_IP }

    private record Rule(HttpMethod method, String pattern, String policy, Subject subject) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/api/v1/auth/login", "login-ip", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/auth/register", "register-ip", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/otp/send", "otp-send-ip", Subject.IP),
            new Rule(HttpMethod.GET, "/api/v1/places/**", "places-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.GET, "/api/v1/garages/nearby", "nearby-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/bookings", "booking-create-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/bookings/phone", "booking-create-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/job-cards/*/media", "media-upload-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/job-cards/*/media/upload-intent", "media-upload-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/navigation/trips/**/media/**", "media-upload-user", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/account/email-verification/send", "otp-send-ip", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/account/email-verification/verify", "otp-verify-ip", Subject.USER_OR_IP),
            new Rule(HttpMethod.POST, "/api/v1/emergency/verify/send", "emergency-ip", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/emergency/verify/confirm", "otp-verify-ip", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/emergency/garages/search", "nearby-user", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/emergency/requests", "emergency-ip", Subject.IP),
            new Rule(HttpMethod.GET, "/api/v1/emergency/requests/*", "emergency-status-ip", Subject.IP),
            new Rule(HttpMethod.POST, "/api/v1/emergency/requests/*/cancel", "emergency-status-ip", Subject.IP));

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Rule rule = match(request);

        if (rule != null) {

            RateLimitService.Decision d = rateLimitService.hit(rule.policy(), subjectOf(rule, request));

            if (!d.allowed()) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(Math.max(1, d.retryAfterSeconds())));
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Too many requests. Please try again later.\","
                                + "\"code\":\"RATE_LIMITED\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Rule match(HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        for (Rule r : RULES) {
            if (r.method().name().equals(method) && matcher.match(r.pattern(), path)) {
                return r;
            }
        }

        return null;
    }

    private static String subjectOf(Rule rule, HttpServletRequest request) {

        if (rule.subject() == Subject.USER_OR_IP) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof GarageUserPrincipal p && p.getId() != null) {
                return "user:" + p.getId();
            }
        }

        return "ip:" + ClientIpResolver.resolve(request);
    }
}
