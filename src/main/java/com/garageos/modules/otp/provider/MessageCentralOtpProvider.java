package com.garageos.modules.otp.provider;

import com.garageos.core.enums.otp.OtpChannel;
import com.garageos.core.enums.otp.OtpPurpose;
import com.garageos.modules.otp.config.OtpProperties;
import com.garageos.modules.otp.exception.OtpException;
import com.garageos.modules.otp.exception.OtpException.Code;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * SMS OTP through Message Central <b>VerifyNow</b>.
 *
 * VerifyNow generates and sends the OTP itself and validates it server-side:
 * {@code POST /verification/v3/send} returns a {@code verificationId}, and
 * {@code GET /verification/v3/validateOtp} checks the code the user typed
 * against it. So this provider reports {@link #validatesCodeItself()}; the
 * OtpService keeps every other rule (expiry, attempt limit, resend cooldown,
 * hourly limit, statuses, error codes) and stores the verificationId in the
 * challenge's provider reference.
 *
 * Authentication: a token from {@code /auth/v1/authentication/token}
 * (customer id + base64 of the account password), cached until shortly before
 * it expires and refreshed once on a 401. The OTP, the token, the password and
 * the mobile number are never logged.
 */
@Slf4j
@Component
public class MessageCentralOtpProvider implements OtpProvider {

    static final String STATUS_COMPLETED = "VERIFICATION_COMPLETED";
    static final int WRONG_OTP = 702;
    static final int ALREADY_VERIFIED = 703;
    static final int VERIFICATION_EXPIRED = 705;

    /** Message Central tokens last 24 h; renew well before that. */
    private static final Duration TOKEN_TTL = Duration.ofHours(23);

    private final OtpProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String token;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public MessageCentralOtpProvider(OtpProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder().connectTimeout(props.getSmsTimeout()).build();
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public String name() {
        return "MESSAGE_CENTRAL";
    }

    @Override
    public boolean validatesCodeItself() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return notBlank(props.getSmsBaseUrl()) && notBlank(props.getSmsCustomerId())
                && notBlank(props.getSmsPassword());
    }

    // -------------------------------------------------------------------- send

    @Override
    public String send(String destination, String otp, OtpPurpose purpose) {

        requireConfigured();

        String url = props.getSmsBaseUrl() + "/verification/v3/send"
                + "?countryCode=" + enc(props.getSmsCountryCode())
                + "&customerId=" + enc(props.getSmsCustomerId())
                + "&flowType=SMS"
                + "&mobileNumber=" + enc(destination)
                + "&otpLength=" + props.getLength();

        JsonNode body = call(authed -> HttpRequest.newBuilder(URI.create(url))
                        .timeout(props.getSmsTimeout())
                        .header("authToken", authed)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                "send", purpose);

        JsonNode data = body.path("data");
        String verificationId = text(data.path("verificationId"));

        if (!isOk(body) || verificationId == null) {
            log.error("[OTP] Message Central did not accept the send. purpose={}, responseCode={}",
                    purpose, body.path("responseCode").asText());
            throw providerFailure(null);
        }

        return verificationId.length() > 100 ? verificationId.substring(0, 100) : verificationId;
    }

    // ---------------------------------------------------------------- validate

    @Override
    public CodeCheck validate(String reference, String destination, String code) {

        requireConfigured();

        if (reference == null || reference.isBlank()) {
            throw providerFailure(null);
        }

        String url = props.getSmsBaseUrl() + "/verification/v3/validateOtp"
                + "?verificationId=" + enc(reference)
                + "&code=" + enc(code)
                + "&flowType=SMS";

        JsonNode body = call(authed -> HttpRequest.newBuilder(URI.create(url))
                        .timeout(props.getSmsTimeout())
                        .header("authToken", authed)
                        .GET()
                        .build(),
                "validate", null);

        return interpretValidation(body);
    }

    /** Package-private for tests: maps a validateOtp response to a {@link CodeCheck}. */
    CodeCheck interpretValidation(JsonNode body) {

        JsonNode data = body.path("data");

        if (isOk(body) && STATUS_COMPLETED.equalsIgnoreCase(text(data.path("verificationStatus")))) {
            return CodeCheck.VALID;
        }

        int code = numeric(body.path("responseCode"));
        if (code == 0 || code == 200) {
            code = numeric(data.path("responseCode"));
        }

        switch (code) {
            case WRONG_OTP:
                return CodeCheck.WRONG;
            case ALREADY_VERIFIED:
                return CodeCheck.ALREADY_VERIFIED;
            case VERIFICATION_EXPIRED:
                return CodeCheck.EXPIRED;
            default:
                // Anything we cannot positively classify must never verify a user.
                log.error("[OTP] Unclassified Message Central validation response. responseCode={}", code);
                throw providerFailure(null);
        }
    }

    // ----------------------------------------------------------------- transport

    @FunctionalInterface
    private interface RequestFactory {
        HttpRequest build(String authToken);
    }

    /**
     * Executes an authenticated call, refreshing the token once on a 401.
     * The body is parsed whatever the HTTP status: Message Central reports
     * wrong/expired codes with a non-200 status and a JSON error code.
     */
    private JsonNode call(RequestFactory factory, String operation, OtpPurpose purpose) {
        try {
            HttpResponse<String> response = http.send(factory.build(token(false)),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 401) {
                response = http.send(factory.build(token(true)),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            }

            JsonNode parsed = parse(response.body());

            if (parsed == null) {
                log.error("[OTP] Message Central {} returned an unreadable body. status={}",
                        operation, response.statusCode());
                throw providerFailure(null);
            }

            if (response.statusCode() / 100 != 2 && numeric(parsed.path("responseCode")) == 0
                    && numeric(parsed.path("data").path("responseCode")) == 0) {
                log.error("[OTP] Message Central {} rejected the request. purpose={}, status={}",
                        operation, purpose, response.statusCode());
                throw providerFailure(null);
            }

            return parsed;

        } catch (IOException e) {
            log.error("[OTP] Message Central unreachable during {}. error={}", operation, e.getClass().getSimpleName());
            throw providerFailure(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw providerFailure(e);
        }
    }

    private synchronized String token(boolean forceRefresh) throws IOException, InterruptedException {

        if (!forceRefresh && token != null && Instant.now().isBefore(tokenExpiresAt)) {
            return token;
        }

        String key = Base64.getEncoder().encodeToString(
                props.getSmsPassword().getBytes(StandardCharsets.UTF_8));

        String url = props.getSmsBaseUrl() + "/auth/v1/authentication/token"
                + "?customerId=" + enc(props.getSmsCustomerId())
                + "&key=" + enc(key)
                + "&scope=NEW"
                + "&country=" + enc(props.getSmsCountryCode());

        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(props.getSmsTimeout())
                        .header("accept", "*/*")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        JsonNode parsed = response.statusCode() / 100 == 2 ? parse(response.body()) : null;
        String fresh = parsed == null ? null : text(parsed.path("token"));

        if (fresh == null) {
            log.error("[OTP] Message Central authentication failed. status={}", response.statusCode());
            this.token = null;
            throw providerFailure(null);
        }

        this.token = fresh;
        this.tokenExpiresAt = Instant.now().plus(TOKEN_TTL);
        return fresh;
    }

    // ------------------------------------------------------------------ helpers

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new OtpException(Code.OTP_NOT_CONFIGURED,
                    "SMS delivery is not configured. Please try again later.");
        }
    }

    private static OtpException providerFailure(Throwable cause) {
        return cause == null
                ? new OtpException(Code.OTP_PROVIDER_FAILURE, "Could not send the OTP right now. Please try again.")
                : new OtpException(Code.OTP_PROVIDER_FAILURE, "Could not send the OTP right now. Please try again.", cause);
    }

    private JsonNode parse(String body) {
        try {
            return body == null || body.isBlank() ? null : mapper.readTree(body);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isOk(JsonNode body) {
        return numeric(body.path("responseCode")) == 200;
    }

    private static int numeric(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        try {
            return Integer.parseInt(node.asText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
