package com.garageos.modules.identity.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The caller's address for throttling, resistant to header spoofing.
 *
 * {@code X-Forwarded-For} is only believed when the TCP peer is itself a private/loopback address (i.e. our own
 * reverse proxy, which is the only thing that can reach the app in that case), and then only its RIGHTMOST entry: that
 * is the address the trusted proxy observed. Anything a client puts to the left of it is ignored, so a caller cannot
 * pick its own bucket by sending a forged header. A peer with a public address is never trusted to supply the header.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return resolve(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
    }

    static String resolve(String peer, String forwardedFor) {

        String direct = peer == null || peer.isBlank() ? "unknown" : peer.trim();

        if (forwardedFor == null || forwardedFor.isBlank() || !isPrivatePeer(direct)) {
            return direct;
        }

        String[] hops = forwardedFor.split(",");
        String last = hops[hops.length - 1].trim();

        return looksLikeIp(last) ? last : direct;
    }

    private static boolean isPrivatePeer(String address) {
        if (!looksLikeIp(address)) {
            return false;
        }
        try {
            InetAddress a = InetAddress.getByName(address);
            return a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                    || (a.getAddress().length == 16 && (a.getAddress()[0] & 0xfe) == 0xfc); // IPv6 unique-local fc00::/7
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** Literal IPv4/IPv6 only - never triggers a DNS lookup. */
    private static boolean looksLikeIp(String s) {
        return s != null && s.length() <= 45 && s.matches("^[0-9a-fA-F:.]+$") && (s.contains(".") || s.contains(":"));
    }
}
