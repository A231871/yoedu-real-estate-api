package com.yoedu.yoedurealestateapi.dto.listingview;

import java.util.UUID;

public record ViewEventPayload(String ipAddress, UUID userId, String userAgent) {

    private static final String SEP = "\u001F";

    public String serialize() {
        return ipAddress + SEP
                + (userId != null ? userId : "") + SEP
                + (userAgent != null ? userAgent : "");
    }

    public static ViewEventPayload deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ViewEventPayload(null, null, null);
        }
        String[] parts = raw.split(SEP, 3);
        String ip = parts.length > 0 && !parts[0].isBlank() ? parts[0] : null;
        UUID userId = null;
        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                userId = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException ignored) {
                userId = null;
            }
        }
        String userAgent = parts.length > 2 ? parts[2] : null;
        return new ViewEventPayload(ip, userId, userAgent);
    }
}
